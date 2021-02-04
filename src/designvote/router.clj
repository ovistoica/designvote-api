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
            [ring.middleware.cors :as cors]))

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
  {;:reitit.middleware/transform dev/print-request-diffs ;; This is for debugging purposes
   :validate  rs/validate
   :exception pretty/exception
   :data      {:coercion   coercion-spec/coercion
               :muuntaja   m/instance
               :middleware [swagger/swagger-feature
                            muuntaja/format-middleware
                            coercion/coerce-request-middleware
                            coercion/coerce-response-middleware
                            mw/exception-middleware]}})

(defn cors-middleware
  "Middleware to allow different origins"
  [handler]
  (cors/wrap-cors handler :access-control-allow-origin [#".*"]
                  :access-control-allow-methods [:get :put :post :delete]))


;#"52.28.45.240",
;#"52.16.224.164",
;#"52.16.193.66",
;#"34.253.4.94",
;#"52.50.106.250",
;#"52.211.56.181",
;#"52.213.38.246",
;#"52.213.74.69",
;#"52.213.216.142",
;#"35.156.51.163",
;#"35.157.221.52",
;#"52.28.184.18",
;#"52.28.212.16",
;#"52.29.176.99",
;#"52.57.230.214",
;#"54.76.184.103",
;#"52.210.122.50",
;#"52.208.95.174",
;#"52.210.122.50",
;#"52.208.95.174",
;#"54.76.184.103"]

(defn routes
  [env]
  (ring/ring-handler
    (ring/router
      [swagger-docs
       ["/v1"
        (design/routes env)
        (account/routes env)
        ]]
      router-config)
    (ring/routes
      (swagger-ui/create-swagger-ui-handler {:path "/"}))
    {:middleware [cors-middleware]}))