(ns designvote.router
  (:require [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.coercion.spec :as coercion-spec]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.spec :as rs]
            [reitit.ring.middleware.dev :as dev]
            [designvote.middleware :as mw]
            [designvote.account.routes :as account]
            [designvote.design.routes :as design]
            [designvote.payment.routes :as payment]
            [ring.middleware.cors :as cors]
            [reitit.exception :as r-exception]))



(def swagger-docs
  ["/swagger.json"
   {:get
    {:no-doc  true
     :swagger {:basePath "/"
               :info     {:title       "Designvote API Reference"
                          :description "The designvote API is organized around REST. Returns JSON, Transit (msgpack, json), or EDN  encoded responses."
                          :version     "1.0.0"}}
     :handler (swagger/create-swagger-handler)}}])

(def router-config
  { :validate  rs/validate
   ;:reitit.middleware/transform dev/print-request-diffs ;; This is for debugging purposes
   :exception pretty/exception
   :conflicts (fn [conflicts]
                (println (r-exception/format-exception :path-conflicts nil conflicts)))
   :data      {:coercion   coercion-spec/coercion
               :muuntaja   m/instance
               :middleware [swagger/swagger-feature
                            muuntaja/format-middleware
                            coercion/coerce-exceptions-middleware
                            coercion/coerce-request-middleware
                            coercion/coerce-response-middleware
                            mw/exception-middleware]}})

(defn cors-middleware
  "Middleware to allow different origins"
  [handler]
  (cors/wrap-cors handler :access-control-allow-origin [#".*"]
                  :access-control-allow-methods [:get :put :post :delete]))

(defn routes
  [env]
  (ring/ring-handler
    (ring/router
      [swagger-docs
       ["/v1"
        (design/routes env)
        (account/routes env)
        (payment/routes env)]]

      router-config)
    (ring/routes
      (swagger-ui/create-swagger-ui-handler {:path "/"})
      ; Finally if nothing matches, return 404
      (constantly {:status 404, :body ""}))
    {:middleware [cors-middleware]}))
