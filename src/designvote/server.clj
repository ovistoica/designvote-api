(ns designvote.server
  (:require [ring.adapter.jetty :as jetty]
            [integrant.core :as ig]
            [environ.core :refer [env]]
            [designvote.router :as router]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as njc]
            [designvote.util :as u])
  (:import
    (com.zaxxer.hikari HikariDataSource)
    (org.eclipse.jetty.server Server)
    (clojure.lang Keyword)
    (java.sql PreparedStatement Timestamp Date Time)
    (org.postgresql.util PGobject)
    (java.util UUID)))

(defn app
  [env]
  (router/routes env))

(defmethod ig/prep-key :server/jetty
  [_ config]
  (merge config {:port (Integer/parseInt (env :port))}))

(defmethod ig/prep-key :db/postgres
  [_ config]
  (merge config {:jdbc-url (env :jdbc-database-url)}))

(defmethod ig/prep-key :auth/auth0
  [_ config]
  (merge config {:client-secret (env :auth0-client-secret)}))

(defmethod ig/init-key :server/jetty
  [_ {:keys [handler port]}]
  (println (str "\nServer running on port " port))
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/init-key :designvote/app
  [_ config]
  (println "\nStarted app")
  (app config))

(defmethod ig/init-key :db/postgres
  [_ {:keys [jdbc-url]}]
  (println (str "\nConfigured db " jdbc-url))
  (jdbc/with-options
    (njc/->pool HikariDataSource {:jdbcUrl jdbc-url :maximumPoolSize 8}) jdbc/unqualified-snake-kebab-opts)
  #_(jdbc/with-options jdbc-url jdbc/unqualified-snake-kebab-opts))


(defmethod ig/halt-key! :db/postgres
  [_ config]
  (.close ^HikariDataSource (:connectable config)))

(defn  write-pg-keyword [^Keyword kw]
  (doto (PGobject.)
    (.setType "text")
    (.setValue (u/keyword->sql-text kw))))

(extend-protocol next.jdbc.prepare/SettableParameter
  Keyword
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (write-pg-keyword m)))
  UUID
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (doto (PGobject.)
                      (.setType "text")
                      (.setValue (str m))))))


(extend-protocol next.jdbc.result-set/ReadableColumn
  ;
  Timestamp
  (read-column-by-label [^Timestamp v _]
    (u/sql-timestamp->inst v))
  (read-column-by-index [^Timestamp v _2 _3]
    (u/sql-timestamp->inst v))
  Date
  (read-column-by-label [^Date v _]
    (.toLocalDate v))
  (read-column-by-index [^Date v _2 _3]
    (.toLocalDate v))
  Time
  (read-column-by-label [^Time v _]
    (.toLocalTime v))
  (read-column-by-index [^Time v _2 _3]
    (.toLocalTime v)))

(defmethod ig/init-key :auth/auth0
  [_ auth0]
  (println "\nConfigured auth0")
  auth0)

(defmethod ig/halt-key! :server/jetty
  [_ ^Server server]
  (.stop server))

(defn -main
  [config-file]
  (let [config (-> config-file slurp ig/read-string)]
    (-> config ig/prep ig/init)))

