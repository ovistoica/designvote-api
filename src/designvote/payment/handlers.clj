(ns designvote.payment.handlers
  (:require [designvote.payment.core :as p]
            [designvote.account.db :as user-db]
            [designvote.util :as u]
            [designvote.config :refer [config]]
            [ring.util.response :as rr]
            [cheshire.core :as json])
  (:import (clojure.lang ExceptionInfo)
           (com.stripe.exception SignatureVerificationException)
           (com.stripe.net Webhook)
           (com.google.gson JsonSyntaxException)))


(defn handle-stripe-error
  [error]
  (clojure.pprint/pprint error)
  (let [status (or (:status error)
                   (get-in error [:response :status])
                   500)
        error-code (or (:error-code error)
                       "internal_server_error")
        message (:message error)]
    {:status status
     :body   {:error-code error-code
              :message    message}}))


(defn create-checkout-session
  "Handler to create checkout session.
  If user does not have a stripe-id, one is created before
  session is created."
  [db]
  (fn [req]
    (let [
          uid (-> req :claims :sub)
          session-info (-> req :parameters :body (u/->kebab-case))
          price-id (if (= "monthly" (:duration session-info)) p/monthly-plan p/yearly-plan)
          user (user-db/get-account db uid)
          stripe-id (or (:stripe-id user)
                        (p/add-stripe-id-to-user! db user))]

      (try
        (let [session (p/create-session (assoc session-info
                                          :price-id price-id
                                          :stripe-id stripe-id
                                          :uid uid))]
          (rr/created (:url session) session))
        (catch ExceptionInfo e
          (let [error (ex-data e)]
            (handle-stripe-error error)))))))


(defn handle-stripe-webhook
  "Handler for stripe events. Currently handling subscription deletion / cancellation"
  [db]
  (fn [req]
    (let [body (:body-params req)
          payload (json/generate-string body)
          sig-header (get (:headers req) "stripe-signature")]

      ; Verify signature from stripe
      (try
        (Webhook/constructEvent payload sig-header p/signing-secret)
        (catch SignatureVerificationException e
          {:status 400
           :body   {:message "Invalid request"}})
        (catch JsonSyntaxException e
          {:status 400
           :body   {:message "Invalid body sent"}}))

      ; Handle different event types
      (case (:type body)
        "customer.subscription.deleted"
        (do
          ; Cancel user subscription
          (let [stripe-id (get-in body [:data :object :customer])]
            (user-db/update-account! db {:stripe-id stripe-id} {:subscription-status :trialing})
            (rr/response {:message "Status returned to :trialing"})))

        "customer.subscription.updated"
        (do
          ; Update the subscription status accordingly
          (let [stripe-id (get-in body [:data :object :customer])
                new-sub-status (get-in body [:data :object :status])]
            (user-db/update-account! db {:stripe-id stripe-id} {:subscription-status new-sub-status})
            (rr/response {:message (str "Status updated to " new-sub-status)})))

        (rr/response {:message "Default in the end"})))))
