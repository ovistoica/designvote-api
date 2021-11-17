(ns designvote.middleware
  (:require [ring.middleware.jwt :as jwt]
            [reitit.ring.middleware.exception :as exception]
            [clojure.pprint :as pp]
            [ring.util.response :as rr]
            [designvote.design.db.core :as design-db]
            [designvote.util :as u])
  (:import (java.sql SQLException)))

(def wrap-auth0
  {:name        ::auth0
   :description "Middleware for auth0 authentication and authorization"
   :wrap        (fn [handler]
                  (jwt/wrap-jwt handler {:alg          :RS256
                                         :jwk-endpoint "https://designvote.eu.auth0.com/.well-known/jwks.json"}))})

(def wrap-authenticated
  {:name ::authenticated
   :description "Middleware to check if user is authenticated. Returns 401 if no token was provided"
   :wrap        (fn [handler]
                  (fn [request]
                    (let [uid (-> request :claims :sub)]
                      (if uid
                        (handler request)
                        (-> (rr/response {:message "You need to be the authenticated for this operation"
                                          :type    :authorization-required})
                            (rr/status 401))))))})


;; type hierarchy
(derive ::error ::exception)
(derive ::failure ::exception)
(derive ::horror ::exception)

(defn handler [message exception request]
  (pp/pprint exception)
  {:status 500
   :body   {:message   message
            :exception (.getClass exception)
            :data      (ex-data exception)
            :uri       (:uri request)}})

(def exception-middleware
  (exception/create-exception-middleware
    (merge
      exception/default-handlers
      {;; ex-data with :type ::error
       ::error             (partial handler "error")

       ;; ex-data with ::exception or ::failure
       ::exception         (partial handler "exception")

       ;; SQLException and all it's child classes
       SQLException        (partial handler "sql-exception")

       ;; override the default handler
       ::exception/default (partial handler "default")

       ;; print stack-traces for all exceptions
       ::exception/wrap    (fn [handler e request]
                             (println "ERROR" (pr-str (:uri request)))
                             (clojure.pprint/pprint e)
                             (handler e request))})))

(def wrap-design-owner
  {:name        ::design-owner
   :description "Middleware to check if a requestor is the recipe owner"
   :wrap        (fn [handler db]
                  (fn [request]
                    (let [uid (-> request :claims :sub)
                          design-id (-> request :parameters :path :design-id)
                          design (design-db/find-design-by-id db design-id)]
                      (if (= (:uid design) uid)
                        (handler request)
                        (-> (rr/response {:message "You need to be the design owner"
                                          :data    (str "design-id " design-id)
                                          :type    :authorization-required})
                            (rr/status 401))))))})

(def wrap-opinion-owner
  {:name        ::design-owner
   :description "Middleware to check if a requestor is the opinion owner"
   :wrap        (fn [handler db]
                  (fn [request]
                    (let [uid (-> request :claims :sub)
                          opinion-id (-> request :parameters :path :opinion-id)
                          opinion (design-db/get-opinion db opinion-id)]
                      (if (= (:uid opinion) uid)
                        (handler request)
                        (-> (rr/response {:message "You need to be the opinion owner"
                                          :data    (str "opinion-id " opinion-id)
                                          :type    :authorization-required})
                            (rr/status 401))))))})


(def wrap-kebab-case
  {:name        ::kebab-case
   :description "Middleware to convert all resources to kebab case"
   :wrap        (fn [handler]
                  (fn [request]
                    (handler (assoc request :parameters
                                            (u/->kebab-case (:parameters request))))))})
