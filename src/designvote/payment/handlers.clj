(ns designvote.payment.handlers
  (:require [designvote.payment.core :as p]
            [designvote.account.db :as user-db]
            [ring.mock.request :as mock]
            [integrant.repl.state :as state]
            [clojure.pprint :as pp]
            [ring.util.response :as rr])
  (:import (clojure.lang ExceptionInfo)))


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
    (let [uid (-> req :claims :sub)
          session-info (-> req :parameters :body)
          user (user-db/get-account db uid)
          stripe-id (or (:stripe-id user)
                        (p/add-stripe-id-to-user! db user))]
      (try
        (let [session (p/create-session (assoc session-info
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
    ; TODO Verify stripe signature
    (let [body (:body-params req)]
      (println (:type body))
      (case (:type body)
        "customer.subscription.deleted"
        (do
          (let [stripe-id (get-in body [:data :object :customer])]
            (user-db/update-account! db {:stripe-id stripe-id} {:subscription-status :trialing})
            (rr/response {:message "Status returned to :trialing"})))

        "customer.subscription.updated"
        (do
          (let [stripe-id (get-in body [:data :object :customer])
                new-sub-status (get-in body [:data :object :status])]
            (user-db/update-account! db {:stripe-id stripe-id} {:subscription-status new-sub-status})
            (rr/response {:message (str "Status updated to " new-sub-status)})))

        (rr/response {:message "Default in the end"})))))


(comment

  (def mock-req
    (assoc (mock/request :post "/v1/payment/checkout")
      :claims {:sub "facebook|5841010855939759"}
      :parameters {:body {:success-url "https://designvote.io"
                          :cancel-url  "https://designvote.io"
                          :price-id    "price_1JCNcwIGGMueBEvzdPAkKP47"}}))


  (def db (-> state/system :db/postgres))

  (def handler (create-checkout-session db))


  (handler mock-req))
