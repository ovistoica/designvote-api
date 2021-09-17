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
            [designvote.payment.handlers :as ph]
            [designvote.account.handlers :as h]
            [clojure.set :refer [rename-keys]]
            [ragtime.repl :as repl]
            [designvote.payment.core :as p]
            [ring.util.response :as rr]))


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






(defn create-design-versions! [db]
  (fn [{{mp :multipart} :parameters}]
    (let [images (:versions mp)
          design-info (dissoc mp :versions)])
    ;TODO Resize and keep quality

    ;TODO Upload images to DO SPaces

    ;TODO See which ones failed

    ;TODO I see t


    (clojure.pprint/pprint mp)
    (rr/response mp)))





(comment

  (def handler (create-design-versions! db))

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
