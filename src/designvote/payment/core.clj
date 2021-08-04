(ns designvote.payment.core
  (:require [clj-stripe.common :as c]
            [clj-stripe.util :as u]
            [clj-stripe.subscriptions :as sub]
            [clj-stripe.customers :as cus]
            [muuntaja.core :as m]
            [clj-stripe.plans :as p]
            [clj-http.client :as http]))

(defonce ^:private token "sk_test_puvyfUhqHSvb0FuWD1w2tE4A00I0zYcd4j")

(defonce ^:private base-url "https://api.stripe.com/v1")

(defonce ^:private cancel-url "https://designvote.io/home")


(defn create-subscription
  "Send api request to create a subscription for a customer in Stripe"
  [customer-id price-id]
  (-> (http/post (str base-url "/subscriptions")
                 {:headers          {"Authorization" (str "Bearer " token)}
                  :throw-exceptions false
                  :form-params      {:customer         customer-id
                                     :payment_behavior "default_incomplete"
                                     "expand[]"        "latest_invoice.payment_intent"
                                     "items[0][price]" price-id}})
      (m/decode-response-body)))



(defn create-costumer
  "Create a new customer inside the Stripe dashboard"
  [{:keys [email name]}]
  (c/with-token token (->> (u/merge-maps
                             (cus/email email)
                             {"name" name})
                           (cus/create-customer)
                           (c/execute))))

(defn create-session
  "Create a payment session"
  [{:keys [success-url cancel-url price-id]}]
  (-> (http/post (str base-url "/checkout/sessions")
                 {:headers          {"Authorization" (str "Bearer " token)}
                  :throw-exceptions false
                  :form-params      {:success_url              success-url
                                     :cancel_url               cancel-url
                                     "payment_method_types[0]" "card"
                                     "line_items[0][price]"    price-id
                                     "line_items[0][quantity]" 1
                                     :mode                     "subscription"}})
      (m/decode-response-body)))



(comment

  (create-session {:success-url "https://designvote.io"
                   :cancel-url  "https://designvote.io"
                   :price-id    "price_1JCNcwIGGMueBEvzdPAkKP47"})

  (create-subscription "cus_Jq82xfYlHv7mGG" "price_1JCNcwIGGMueBEvzdPAkKP47"))












