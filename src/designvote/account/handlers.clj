(ns designvote.account.handlers
  (:require [ring.util.response :as rr]
            [designvote.account.db :as account-db]
            [clj-http.client :as http]
            [designvote.auth0 :as auth0]
            [muuntaja.core :as m]
            [designvote.responses :as responses]))

;; After an account has been created in auth0, store it
;; in the local DB. Also used in local tests
(defn create-account-from-uid!
  [db auth0]
  (fn [request]
    (let [{:keys [sub]} (-> request :claims)
          {:keys [name email picture]} (->>
                                         {:headers {"Authorization" (str "Bearer "
                                                                         (auth0/get-management-token auth0))}}
                                         (http/get (str "https://designvote.eu.auth0.com/api/v2/users/" sub))
                                         (m/decode-response-body))]
      (account-db/create-account! db {:uid sub :name name :email email :picture picture})
      (rr/status 201))))

;; Webhook endpoint. After auth0 created a new account
;; it send the user info at this endpoint to be stored in the DB
(defn create-account!
  [db]
  (fn [request]
    (let [body (-> request :parameters :body)
          user (dissoc body :token)
          token (:token body)]
      (if (= token "VIZFDFAlCzwze9g")
        (do (account-db/create-account! db user)
            (rr/created (str responses/base-url "/accounts/" (:uid user)) user))
        {:status  401
         :headers {}
         :body    {:message "Unauthorised"}}))))




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
