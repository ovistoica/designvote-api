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
            [designvote.payment.core :as p]))


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

  (repl/migrate config)


  (adb/create-account! db {:email "test@awdawd.com"
                           :name  "test"
                           :uid   "awdawd"})

  (def handler (h/get-account db))
  (def check-h (ph/create-checkout-session db))


  (handler {})

  (adb/get-account db "google-oauth2|117984597083645660112")
  (p/add-stripe-id-to-user! db (adb/get-account db "facebook|5841010855939759"))
  (adb/get-account db "facebook|5841010855939759")

  (adb/update-account! db {:uid "facebook|5841010855939759"} {:subscription-status :trialing})
  (def handler (h/create-account! db))


  (sql/find-by-keys db :account {:uid "this_is_a_test3"})

  (handler request)

  (p/add-stripe-id-to-user! db (adb/get-account db "google-oauth2|117984597083645660112"))

  (ddb/count-user-designs db "google-oauth2|117984597083645660112")
  (ddb/find-all-user-designs! db "google-oauth2|117984597083645660112"))
