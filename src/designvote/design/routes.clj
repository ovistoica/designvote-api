(ns designvote.design.routes
  (:require [designvote.middleware :as mw]
            [designvote.design.handlers :as design]
            [designvote.responses :as responses]
            [clojure.spec.alpha :as s]))

(defn routes
  [env]
  (let [db (:jdbc-url env)]
    ["/designs" {:swagger {:tags ["designs"]}}
     [""
      {:middleware [[mw/wrap-auth0]]
       :get        {:handler   (design/list-all-designs! db)
                    :responses {200 {:body responses/designs}}
                    :summary   "Get all designs"}
       :post       {:handler    (design/create-design! db)
                    :responses  {201 {:body {:design-id string?}}}
                    :parameters {:body {:name        string?
                                        :description (s/nilable string?)
                                        :question    (s/nilable string?)
                                        :img         (s/nilable string?)
                                        :design-type string?
                                        :vote-style  string?}}
                    :summary    "Create a design"}
       }]
     ["/short"
      ["/:short-url"
       {:get {:handler    (design/find-design-by-url! db)
              :responses  {200 {:body responses/design}}
              :parameters {:path {:short-url string?}}
              :summary    "Retrieve design by short url"}}
       ]]
     ["/:design-id"
      [""
       {:middleware [[mw/wrap-auth0]]
        :get        {:handler    (design/retrieve-design! db)
                     :responses  {200 {:body responses/design}}
                     :parameters {:path {:design-id string?}}
                     :summary    "Retrieve design"}
        :put        {:handler    (design/update-design! db)
                     :middleware [[mw/wrap-design-owner db]]
                     :responses  {204 {:body nil?}}
                     :parameters {:path {:design-id string?}
                                  :body {:name        (s/nilable string?)
                                         :description (s/nilable string?)
                                         :img         (s/nilable string?)
                                         :public      (s/nilable boolean?)
                                         :design-type (s/nilable string?)}}
                     :summary    "Update design"}
        :delete     {:handler    (design/delete-design! db)
                     :middleware [[mw/wrap-design-owner db]]
                     :responses  {204 {:body nil?}}
                     :parameters {:path {:design-id string?}}
                     :summary    "Delete design"}}]
      ["/publish"
       {:middleware [[mw/wrap-auth0]]
        :post       {:handler    (design/publish-design! db)
                     :response   {201 {:body {:design-id string?}}}
                     :parameters {:path {:design-id string?}}
                     :summary    "Publish a design to be ready for voting"}}

       ]
      ["/versions" {:middleware [[mw/wrap-auth0] [mw/wrap-design-owner db]]}
       [""
        {:post   {:handler    (design/add-design-version! db)
                  :responses  {201 {:body {:version-id string?}}}
                  :parameters {:path {:design-id string?}
                               :body {:name        string?
                                      :pictures    vector?
                                      :description (s/nilable string?)}}
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
                  :summary    "Delete a design version"}}]
       ["/multiple"
        {:post {:handler    (design/add-multiple-design-versions db)
                :response   {201 {:body {:design-id string?}}}
                :parameters {:path {:design-id string?}
                             :body {:versions [{:name        string?
                                                :pictures    vector?
                                                :description (s/nilable string?)}]}}
                :summary    "Upload multiple design versions"}}
        ]
       ]
      ["/votes"
       {:post   {:handler    (design/vote-design! db)
                 :parameters {:path {:design-id string?}
                              :body {:vote-style string?
                                     :version-id string?
                                     :rating     (s/nilable number?)
                                     :voter-id   string?}}
                 :responses  {204 {:body nil?}}
                 :summary    "Vote design version"}
        :delete {:handler    (design/unvote-design! db)
                 :parameters {:path {:design-id string?}
                              :body {:version-id string?}}
                 :responses  {204 {:body nil?}}
                 :summary    "Unvote design version"}}]]

     ]))

