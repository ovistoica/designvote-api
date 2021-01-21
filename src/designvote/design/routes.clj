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
                 :responses  {204 {:body nil?}}
                 :parameters {:path {:design-id string?}
                              :body {:name        string?
                                     :description string?
                                     :img         string?}}
                 :summary "Update design"}
        :delete {:handler    (design/delete-design! db)
                 :responses  {204 {:body nil?}}
                 :parameters {:path {:design-id string?}}
                 :summary "Delete design"}}]
      ["/options" {:middleware [[mw/wrap-design-owner db]]}
       [""
        {:post {:handler    (design/add-design-option! db)
                :responses  {201 {:body {:option-id string?}}}
                :parameters {:body {:name        string?
                                    :pictures    vector?
                                    :description string?}}
                :summary    "Create a design option"}
         :put  {:handler    (design/update-design-option! db)
                :responses  {204 {:body nil?}}
                :parameters {:body {:option-id   string?
                                    :name        string?
                                    :picture     vector?
                                    :description string?}}
                :summary    "Update a design option"}}]]]]))

