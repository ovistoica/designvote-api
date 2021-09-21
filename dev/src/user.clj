(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [integrant.repl.state :as state]
            [designvote.server]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [ragtime.jdbc :as rjdbc]
            [designvote.account.db :as adb]
            [designvote.design.db :as ddb]
            [designvote.design.handlers :as dh]
            [designvote.payment.handlers :as ph]
            [designvote.account.handlers :as h]
            [clojure.java.io :as io]
            [clojure.set :refer [rename-keys]]
            [ragtime.repl :as repl]
            [designvote.payment.core :as p]
            [ring.mock.request :as mock]))


(ig-repl/set-prep!
  (fn [] (-> "dev/resources/config.edn" slurp ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def restart ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :designvote/app))
(def db (-> state/system :db/postgres))

; config for db migrations
(def db-url (-> "dev/resources/config.edn" slurp ig/read-string :db/postgres :jdbc-url))
(def config
  {:datastore  (rjdbc/sql-database {:connection-uri db-url})
   :migrations (rjdbc/load-resources "migrations")
   :reporter   println})

(comment
  (repl/migrate config))

(comment

  (def versions [{:tempfile (io/file "resources/lion_king1.jpeg")}
                 {:tempfile (io/file "resources/lion_king2.jpeg")}
                 {:tempfile (io/file "resources/lion_king3.jpeg")}])

  (def req (assoc (mock/request :post "/v2/designs")
             :claims {:sub "google-oauth2|117984597083645660112"}
             :parameters {:multipart {:versions versions
                                      :name "test-full-design"
                                      :question "Is this design full?"
                                      :design-type "logo"
                                      :is-public true}}))

  (ddb/get-latest-public-designs-paginated db)
  (ddb/find-design-by-id db "0b423b56-9470-4c5b-9447-88d5d17267a7")

  (def handler (dh/create-design-with-versions! db))
  (handler req)

  (def mock-req
    (assoc (mock/request :post "/v1/design/DESIGN_ID_TO_CHANGE/versions/multiple")
      :claims {:sub "facebook|5841010855939759"}
      :parameters {})))







;; Account testing
(comment
  (adb/get-account db "google-oauth2|117984597083645660112")
  (adb/get-account db "facebook|5841010855939759")

  (adb/update-account! db {:uid "facebook|5841010855939759"} {:subscription-status :trialing}))







;; Test functions for payment
(comment

  (p/add-stripe-id-to-user! db (adb/get-account db "facebook|5841010855939759"))

  (def mock-req
    (assoc (mock/request :post "/v1/payment/checkout")
      :claims {:sub "facebook|5841010855939759"}
      :parameters {:body {:success-url "https://designvote.io"
                          :cancel-url  "https://designvote.io"
                          :price-id    "price_1JCNcwIGGMueBEvzdPAkKP47"}}))

  (def db (-> state/system :db/postgres))

  (def handler (create-checkout-session db))

  (handler mock-req))
