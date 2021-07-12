(ns designvote.server
  (:require [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [integrant.core :as ig]
            [environ.core :refer [env]]
            [designvote.router :as router]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as njc])
  (:import
    (com.zaxxer.hikari HikariDataSource)
    (org.eclipse.jetty.server Server)))

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

(comment
  (app {:request-method :get
        :uri            "/"})
  (-main))
