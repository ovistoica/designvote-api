(ns designvote.payment.routes
  (:require [designvote.payment.handlers :as h]
            [clojure.spec.alpha :as s]
            [designvote.middleware :as mw]))

(defn routes
  [env]
  (let [db (:jdbc-url env)]
    ["/payment" {:swagger {:tags ["payment"]}}
     ["/checkout"
      {:post {:middleware [mw/wrap-auth0]
              :summary    "Create a checkout session for the user"
              :responses  {201 {:body map?}}
              :parameters {:body {:success_url string?
                                  :cancel_url  string?
                                  :duration    string?}}
              :handler    (h/create-checkout-session db)}}]
     ["/webhook"
      {:post {:summary "Webhook for stripe asynchronous events"
              :handler (h/handle-stripe-webhook db)}}]]))


