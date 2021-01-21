(ns designvote.auth0
  (:require [clj-http.client :as http]
            [muuntaja.core :as m]))



(defn get-management-token
  [auth0]
  (->> {:content-type     :json
        :throw-exceptions false
        :cookie-policy    :standard
        :body             (m/encode "application/json"
                                    {
                                     :client_id     "Z9RAq7kCWXZvuqNmYLakKEtpt4CCpATv"
                                     :client_secret (:client-secret auth0)
                                     :audience      "https://designvote.eu.auth0.com/api/v2/"
                                     :grant_type    "client_credentials"
                                     })}
       (http/post "https://designvote.eu.auth0.com/oauth/token")
       (m/decode-response-body)
       :access_token))




(comment
  (get-management-token)
  (get-manage-recipe-role)
  (get-test-token)
  (create-auth0-user {:connection "Username-Password-Authentication"
                      :email      "account-testing@cheffy.app"
                      :password   "Sepulcral94"}))