(ns designvote.test-system
  (:require [clojure.test :refer :all]
            [integrant.repl.state :as state]
            [ring.mock.request :as mock]
            [muuntaja.core :as m]
            [clj-http.client :as http]
            [designvote.auth0 :as auth0]))

(def token (atom nil))

(defn get-test-token
  [email]
  (->> {:content-type  :json
        :throw-exceptions false
        :cookie-policy :standard
        :body          (m/encode "application/json"
                                 {:client_id  "Fx9nSRZtW7L5Rk9tiRZT1bl97RoK1b1H"
                                  :audience   "https://designvote.eu.auth0.com/api/v2/"
                                  :grant_type "password"
                                  :username   email
                                  :password   "Sepulcral94"
                                  :scope      "openid profile email"})}
       (http/post "https://designvote.eu.auth0.com/oauth/token")
       (m/decode-response-body)
       :access_token))

(defn create-auth0-user
  [{:keys [connection email password]}]
  (let [auth0 (-> state/system :auth/auth0)]
    (->> {:headers          {"Authorization" (str "Bearer " (auth0/get-management-token auth0))}
          :content-type     :json
          :throw-exceptions false
          :cookie-policy    :standard
          :body             (m/encode "application/json"
                                      {:connection connection
                                       :email      email
                                       :password   password})
          }
         (http/post "https://designvote.eu.auth0.com/api/v2/users")
         (m/decode-response-body))))


(defn test-endpoint
  ([method uri]
   (test-endpoint method uri nil))
  ([method uri opts]
   (let [app (-> state/system :designvote/app)
         request (app (-> (mock/request method uri)
                          (cond-> (:auth opts)
                                  (mock/header :authorization (str "Bearer "
                                                                   (or @token (get-test-token "account-testing@designvote.io"))))
                                  (:body opts) (mock/json-body (:body opts)))))]
     (update request :body (partial m/decode "application/json")))))

(comment
  (let [request (test-endpoint :get "/v1/recipes")
        decoded-request (m/decode-response-body request)]
    (assoc request :body decoded-request))
  (test-endpoint :post "/v1/recipes" {:img       "string"
                                      :name      "my name"
                                      :prep-time 30})


  (test-endpoint :get "/v1/designs" {:auth true })
  (test-endpoint :post "/v1/designs" {:auth true :body {:name        "My new design"
                                                        :description "Helooo design"
                                                        :img         "My image"
                                                        }})
  (test-endpoint :get "/v1/designs/89985a6c-6864-4a43-9e90-b92aee727048")
  (get-test-token "testing@designvote.io")
  (create-auth0-user {:connection "Username-Password-Authentication"
                     :email      "account-testing@designvote.io"
                     :password   "Sepulcral94"}))