(ns designvote.account.routes
  (:require [designvote.middleware :as mw]
            [designvote.account.handlers :as account]))

(defn routes
  [env]
  (let [db (:jdbc-url env)
        auth0 (:auth0 env)]
    ["/account" {:swagger    {:tags ["account"]} }
     [""
      {:post   {:handler   (account/create-account! db auth0)
                :responses {201 {:body any?}}
                :parameters {:body any?}
                :summary   "Create an account"}
       :delete {:handler   (account/delete-account! db auth0)
                :responses {204 {:body nil?}}
                :summary   "Delete an account"}}]]))

