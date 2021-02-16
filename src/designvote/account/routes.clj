(ns designvote.account.routes
  (:require [designvote.middleware :as mw]
            [designvote.account.handlers :as account]))

(defn routes
  [env]
  (let [db (:jdbc-url env)
        auth0 (:auth0 env)]
    ["/account" {:swagger {:tags ["account"]} }
     [""
      {:post   {:handler    (account/create-account! db)
                :responses  {201 {:body any?}}
                :parameters {:body {:email    string?
                                    :social   boolean?
                                    :uid      string?
                                    :name     string?
                                    :nickname string?
                                    :picture  string?
                                    :provider string?
                                    :token    string?}}
                :summary    "Create an account"}
       :delete {:handler   (account/delete-account! db auth0)
                :responses {204 {:body nil?}}
                :middleware [[mw/wrap-auth0]]
                :summary   "Delete an account"}}]
     ["/uid"
      {:post {:handler   (account/create-account-from-uid! db auth0)
              :responses {201 {:body nil}}
              :middleware [[mw/wrap-auth0]]
              :summary   "Create an account based on user token and id"}}]]))

