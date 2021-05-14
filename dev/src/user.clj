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
            [clojure.set :refer [rename-keys]]
            [ragtime.repl :as repl]))


(ig-repl/set-prep!
  (fn [] (-> "dev/resources/config.edn" slurp ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :designvote/app))
(def db (-> state/system :db/postgres))


; config for db migrations
(def db-url (-> "dev/resources/config.edn" slurp ig/read-string :db/postgres :jdbc-url))
(def config
  {:datastore  (rjdbc/sql-database {:connection-uri db-url})
   :migrations (rjdbc/load-resources "migrations")
   :reporter   println})

(def vote-query
  {:version-id "82d35d4e-4474-4a89-bdc1-0649e368ee6f"
   :uid        "anonymous|6d18ccf5-1f54-413b-8d67-9f68a31da5a4"}
  )

(def test-ratings {"82d35d4e-4474-4a89-bdc1-0649e368ee6f" 12
                   "82d35d4e-4474-4a89-bdc1-0649e368ee6a" 14})


;[{:rating     rating
;  :design-id  design-id
;  :version-id version-id
;  :voter-name voter-name}]

(defn insert-feedback!
  [{:keys [design-id ratings comments voter-name]}]
  (let [])
  )

(comment
  (let [res (-> test :body
                (into {} (remove (comp nil? val)))
                )]
    (res))
  (let [kv-pairs (vec test-ratings)
        db-ratings (into [] (for [[k v] kv-pairs] {:version-id k
                                                   :rating     v}))]
    (println db-ratings)
    db-ratings)

  (let [db-ratings (-> test-ratings
                       (vec)
                       (map #({:version-id (first %)
                               :rating     (last %)}))
                       )]
    db-ratings)

  (if-let [[existent-vote] (sql/find-by-keys db :vote vote-query)]
    (print "found it" existent-vote)
    (print "didnt find it"))


  (repl/migrate config)

  )
