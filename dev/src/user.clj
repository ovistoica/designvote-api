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
            [ragtime.repl :as repl]))


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
   :migrations (rjdbc/load-resources "migrations")
   :reporter   println})

(println (:connectable db))

(def vote-query
  {:version-id "82d35d4e-4474-4a89-bdc1-0649e368ee6f"
   :uid   "anonymous|6d18ccf5-1f54-413b-8d67-9f68a31da5a4"}
  )




(comment
  (let [res (-> test :body
                (into {} (remove (comp nil? val)))
                )]
    (res))

  (if-let [[existent-vote] (sql/find-by-keys db :vote vote-query)]
    (print "Found it" existent-vote)
    (print "Didnt find it"))


  (repl/migrate config)


  )
