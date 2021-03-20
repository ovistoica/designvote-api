(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [integrant.repl.state :as state]
            [designvote.server]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [designvote.design.db :as design-db]
            [buddy.core.hash :as h]
            [buddy.core.codecs :as c]
            [ragtime.jdbc :as rjdbc]
            [ragtime.repl :as repl] ))


(ig-repl/set-prep!
  (fn [] (-> "dev/resources/config.edn" slurp ig/read-string)))
(def test {:a    "h"
           :body {
                  :a "heoi"
                  :b nil
                  :c "12"
                  }})

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :designvote/app))
(def db (-> state/system :db/postgres))


; Config for db migrations
(def db-url (-> "dev/resources/config.edn" slurp ig/read-string :db/postgres :jdbc-url))
(def config
  {:datastore  (rjdbc/sql-database {:connection-uri db-url})
   :migrations (rjdbc/load-resources "migrations")})

(println (:connectable db))





(comment
  (let [res (-> test :body
                (into {} (remove (comp nil? val)))
                )]
    (res))


  (into {} (remove (comp nil? val) (:body test)))
  (repl/migrate config)
  )