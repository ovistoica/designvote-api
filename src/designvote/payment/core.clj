(ns designvote.payment.core
  (:require [muuntaja.core :as m]
            [clj-http.client :as http]
            [designvote.account.db :as user-db]
            [designvote.util :as u]
            [designvote.config :refer [config]]
            [clojure.tools.logging :as log]))

(def ^:private ^String token (:stripe-secret config))
(def ^:private ^String stripe-api-base-url "https://api.stripe.com/v1")

(def  ^String monthly-plan (:monthly-plan config))
(def  ^String yearly-plan (:yearly-plan config))
(def  ^String signing-secret (:signing-secret config))

(def subscription-status #{:trialing :active :past_due :canceled :unpaid})


(defn- handle-error [body]
  (let [invalid-token? (= (get-in body [:error :type]) "invalid_auth")
        message (if invalid-token?
                  "Invalid token"
                  (str "Stripe API error: " (:type (:error body))))
        error (if invalid-token?
                {:error-code (:type (:error body))
                 :errors     {:stripe-token message}}
                {:error-code (:type (:error body))
                 :message    message
                 :response   body})]
    (log/warn (u/pprint-to-str error))
    (throw (ex-info message error))))

(defn handle-response [{:keys [status] :as response}]
  (let [body (m/decode-response-body response)]
    (if (< status 300)
      (assoc body :status status)
      (handle-error (assoc body :status status)))))


(defn- do-stripe-request [request-fn endpoint config]
  (let [token (or (get-in config [:query-params :token])
                  (get-in config [:form-params :token])
                  token)]
    (when token
      (let [url (str stripe-api-base-url "/" (name endpoint))
            _ (log/trace "Slack API request: %s %s" (pr-str url) (pr-str config))
            request (merge-with merge
                                {:headers          {"Authorization" (str "Bearer " token)}
                                 :throw-exceptions false
                                 ;; use a relatively long connection timeout (10 seconds) in cases where we're fetching big
                                 ;; amounts of data -- see #11735
                                 :conn-timeout     10000
                                 :socket-timeout   10000}
                                config)]
        (try
          (handle-response (request-fn url request))
          (catch Throwable e
            (throw (ex-info (.getMessage e) (merge (ex-data e) {:url url, :request request}) e))))))))



(defn- GET
  "Make a GET request to the Stripe API."
  [endpoint query-params]
  (do-stripe-request http/get endpoint {:query-params query-params}))

(defn- POST
  "Make a POST request to the Stripe API."
  [endpoint body]
  (do-stripe-request http/post endpoint {:form-params body}))


(defn create-subscription
  [{:keys [customer-id price-id]}]
  (POST :subscriptions {:customer         customer-id
                        :payment_behavior "default_incomplete"
                        "expand[]"        "latest_invoice.payment_intent"
                        "items[0][price]" price-id}))

(defn retrieve-subscription
  [customer-id]
  (GET :subscriptions {:customer customer-id}))


(defn create-costumer
  "Create a new customer inside the Stripe dashboard"
  [{:keys [email name uid]}]
  (POST :customers {:name           name
                    :email          email
                    :description    "(created by Designvote API)"
                    "metadata[uid]" uid}))

(defn create-session
  "Create a payment session"
  [{:keys [success-url cancel-url price-id uid stripe-id]}]
  (POST "checkout/sessions" {:success_url              success-url
                             :cancel_url               cancel-url
                             :client_reference_id      uid
                             :customer                 stripe-id
                             "payment_method_types[0]" "card"
                             "line_items[0][price]"    price-id
                             "line_items[0][quantity]" 1
                             :mode                     "subscription"}))

(defn active-subscription?
  "Check if the customer has an active subscription"
  [customer-id]
  (-> (retrieve-subscription customer-id)
      :data
      (first)
      :status
      (= "active")))

(defn add-stripe-id-to-user!
  [db {:keys [uid name email]}]
  (let [stripe-id (:id (create-costumer {:email email
                                         :name  name
                                         :uid   uid}))
        updated? (-> (user-db/update-account! db {:uid uid} {:stripe-id stripe-id})
                     :next.jdbc/update-count
                     (pos?))]
    stripe-id))
