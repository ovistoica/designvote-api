(ns designvote.account.handlers
  (:require [ring.util.response :as rr]
            [designvote.account.db :as account-db]
            [clj-http.client :as http]
            [designvote.auth0 :as auth0]
            [muuntaja.core :as m]))

;; After an account has been created in auth0, store it
;; in the local DB
(defn create-account!
  [db auth0]
  (fn [request]
    (let [{:keys [sub]} (-> request :claims)
          {:keys [name email picture]} (->>
                    {:headers {"Authorization" (str "Bearer "
                                                    (auth0/get-management-token auth0))}}
                    (http/get (str "https://designvote.eu.auth0.com/api/v2/users/" sub))
                    (m/decode-response-body))]
      (account-db/create-account! db {:uid sub :name (or name email) :picture picture})
      (rr/status 201))))


;; Make a request to delete account from auth0
;; and then delete it from the local DB
(defn delete-account!
  [db auth0]
  (fn [request]
    (let [uid (-> request :claims :sub)
          deleted-auth0-account! (http/delete
                                   (str "https://designvote.eu.auth0.com/api/v2/users/" uid)
                                   {:headers {"Authorization" (str "Bearer "
                                                                   (auth0/get-management-token auth0))}})]
      (when (= (:status deleted-auth0-account!) 204)
        (account-db/delete-account! db {:uid uid})
        (rr/status 204)))))