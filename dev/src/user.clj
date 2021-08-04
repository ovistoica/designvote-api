(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [integrant.repl.state :as state]
            [designvote.server]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [ragtime.jdbc :as rjdbc]
            [designvote.account.db :as adb]
            [designvote.account.handlers :as h]
            [clojure.set :refer [rename-keys]]
            [ragtime.repl :as repl]))


(ig-repl/set-prep!
  (fn [] (-> "dev/resources/config.edn" slurp ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def restart ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :designvote/app))
(def db (-> state/system :db/postgres))

db


; config for db migrations
(def db-url (-> "dev/resources/config.edn" slurp ig/read-string :db/postgres :jdbc-url))
(def config
  {:datastore  (rjdbc/sql-database {:connection-uri db-url})
   :migrations (rjdbc/load-resources "migrations")
   :reporter   println})

(def vote-query
  {:version-id "82d35d4e-4474-4a89-bdc1-0649e368ee6f"
   :uid        "anonymous|6d18ccf5-1f54-413b-8d67-9f68a31da5a4"})


(def test-ratings {"82d35d4e-4474-4a89-bdc1-0649e368ee6f" 12
                   "82d35d4e-4474-4a89-bdc1-0649e368ee6a" 14})


;[{:rating     rating
;  :design-id  design-id
;  :version-id version-id
;  :voter-name voter-name}]

(defn insert-feedback!
  [{:keys [design-id ratings comments voter-name]}]
  (let []))


(comment

  (repl/migrate config)


  (adb/create-account! db {:email "test@awdawd.com"
                           :name "test"
                           :uid "awdawd"})


  (def handler (h/create-account! db))

  (def request {:parameters {:body {:token "VIZFDFAlCzwze9g"
                                    :uid "this_is_a_test3"
                                    :email "test_new_handler@test3.com"
                                    :name "Test Handler"}}})

  (sql/find-by-keys db :account {:uid "this_is_a_test3"})

  (handler request)

  (design-db/find-all-user-designs! db "google-oauth2|117984597083645660112"))



  ;

