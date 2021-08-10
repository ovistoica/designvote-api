(ns designvote.payment.handlers
  (:require [designvote.payment.core :as p]
            [designvote.account.db :as user-db]
            [ring.mock.request :as mock]
            [integrant.repl.state :as state]
            [clojure.pprint :as pp]
            [ring.util.response :as rr]))




(defn create-checkout-session
  "Handler to create checkout session.
  If user does not have a stripe-id, one is created before
  session is created."
  [db]
  (fn [req]
    (let [uid (-> req :claims :sub)
          session-info (-> req :parameters :body)
          user (user-db/get-account db uid)]
      (when-not (:stripe-id user)
        (p/add-stripe-id-to-user! db user))
      (try
        (rr/response (p/create-session session-info))
        (catch Exception e
          (let [error (ex-data e)]
            (if-let [status (:status error)]
              {:status status
               :body   (:body error)}
              {:status 500
               :body   {:message "Something went wrong. Please try again"}})))))))


(defn create-subscription
  "Handler to add the subscription-id to the user account"
  [db]
  (fn [req]
    (let [uid (-> req :claims :sub)
          subscription (-> req :parameters :body)
          user (user-db/get-account db uid)]
      (when-not (:stripe-id user)
        {:status 401
         :body   {:message "Unauthorized! User does not have a valid stripe-id"}})
      (try
        ; TODO See if this is needed
        (catch Exception e
          (let [error (ex-data e)]
            (if-let [status (:status error)]
              {:status status
               :body   (:body error)}
              {:status 500
               :body   {:message "Something went wrong. Please try again"}})))))))



(defn handle-stripe-webhook
  "Useful for subscription readiness"
  [db]
  (fn [req]
    (let [body (:body-params req)]
      (println (:type body))
      (case (:type body)

        "customer.subscription.created"
        (let [customer (get-in body [:data :object :customer])]
          (println (str "Subscription created" " by " "customer" customer))
          (rr/response {:message (str "Success" customer)}))

        "invoice.payment_succeeded"
        (let [customer (get-in body [:data :object :customer])]
          (println (str "Subscription created" " by " "customer" customer))
          (rr/response {:message (str "Success" customer)}))
        (rr/response {:message "Default in the end"})))))




;(case (:type event)
;  "customer.subscription.created"))))




(comment


  (def mock-req
    (assoc (mock/request :post "/v1/payment/checkout")
      :claims {:uid "google-oauth2|117984597083645660112"}
      :parameters {:body {:success-url "https://designvote.io"
                          :cancel-url  "https://designvote.io"
                          :price-id    "price_1JCNcwIGGMueBEvzdPAkKP47"}}))

  (def db (-> state/system :db/postgres))

  (def handler (create-checkout-session db))
  (handler mock-req))















