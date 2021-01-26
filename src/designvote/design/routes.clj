(ns designvote.design.routes
  (:require [designvote.middleware :as mw]
            [designvote.design.handlers :as design]
            [designvote.responses :as responses]))

(defn routes
  [env]
  (let [db (:jdbc-url env)]
    ["/designs" {:swagger    {:tags ["designs"]}
                 :middleware [[mw/wrap-auth0]]}
     [""
      {:get  {:handler   (design/list-all-designs! db)
              :responses {200 {:body responses/designs}}
              :summary   "Get all designs"}
       :post {:handler    (design/create-design! db)
              :responses  {201 {:body {:design-id string?}}}
              :parameters {:body {:name        string?
                                  :description string?
                                  :img         string?}}
              :summary    "Create a design"}
       }]
     ["/:design-id"
      [""
       {:get    {:handler    (design/retrieve-design! db)
                 :responses  {200 {:body responses/design}}
                 :parameters {:path {:design-id string?}}
                 :summary    "Retrieve design"}
        :put    {:handler    (design/update-design! db)
                 :middleware [[mw/wrap-design-owner db]]
                 :responses  {204 {:body nil?}}
                 :parameters {:path {:design-id string?}
                              :body {:name        string?
                                     :description string?
                                     :img         string?
                                     :public      boolean?}}
                 :summary    "Update design"}
        :delete {:handler    (design/delete-design! db)
                 :middleware [[mw/wrap-design-owner db]]
                 :responses  {204 {:body nil?}}
                 :parameters {:path {:design-id string?}}
                 :summary    "Delete design"}}]
      ["/versions" {:middleware [[mw/wrap-design-owner db]]}
       [""
        {:post   {:handler    (design/add-design-version! db)
                  :responses  {201 {:body {:version-id string?}}}
                  :parameters {:path {:design-id string?}
                               :body {:name        string?
                                      :pictures    vector?
                                      :description string?}}
                  :summary    "Create a design version"}
         :put    {:handler    (design/update-design-version! db)
                  :responses  {204 {:body nil?}}
                  :parameters {:path {:design-id string?}
                               :body {:version-id  string?
                                      :name        string?
                                      :description string?}}
                  :summary    "Update a design version"}
         :delete {:handler    (design/delete-design-version! db)
                  :responses  {204 {:body nil?}}
                  :parameters {:path {:design-id string?}
                               :body {:version-id string?}}
                  :summary    "Delete a design version"}}]]
      ["/votes"
       {:post   {:handler    (design/vote-design! db)
                 :parameters {:path {:design-id string?}
                              :body {:version-id string?
                                     :opinion string?}}
                 :responses  {204 {:body nil?}}
                 :summary    "Vote design version"}
        :delete {:handler    (design/unvote-design! db)
                 :parameters {:path {:design-id string?}
                              :body {:version-id string?}}
                 :responses  {204 {:body nil?}}
                 :summary    "Unvote design version"}}]]]))

