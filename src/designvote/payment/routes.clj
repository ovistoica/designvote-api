(ns designvote.payment.routes
  (:require [designvote.payment.handlers :as h]
            [clojure.spec.alpha :as s]))

(defn routes
  [env]
  (let [db (:jdbc-url env)]
    ["/payment" {:swagger {:tags ["payment"]}}
     ["/checkout"
      {:post {:summary    "Create a checkout session"
              ;:responses  {201 {:body {:message string?}}}
              :parameters {:body {:success-url string?
                                  :cancel-url  string?
                                  :price-id    string?}}
              :handler    (h/create-checkout-session db)}}]
     ["/webhook"
      {:post {:summary "Webhook for stripe asynchronous events"
              :handler (h/handle-stripe-webhook db)}}]]))

