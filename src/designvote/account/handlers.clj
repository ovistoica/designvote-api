(ns designvote.account.handlers
  (:require [ring.util.response :as rr]
            [designvote.account.db :as account-db]
            [clj-http.client :as http]
            [designvote.auth0 :as auth0]
            [muuntaja.core :as m]))

;; After an account has been created in auth0, store it
;; in the local DB
(defn create-account!
  [db]
  (fn [request]
    (let [{:keys [sub name picture]} (-> request :claims)]
      (account-db/create-account! db {:uid sub :name name :picture picture})
      (rr/status 201))))


;; Make a request to delete account from auth0
;; and then delete it from the local DB
(defn delete-account!
  [db auth0]
  (fn [request]
    (let [uid (-> request :claims :sub)
          deleted-auth0-account! (http/delete
                                   (str "https://dev-ovidiu.eu.auth0.com/api/v2/users/" uid)
                                   {:headers {"Authorization" (str "Bearer " (auth0/get-management-token auth0))}})]
      (when (= (:status deleted-auth0-account!) 204)
        (account-db/delete-account! db {:uid uid})
        (rr/status 204)))))