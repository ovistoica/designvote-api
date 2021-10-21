(ns designvote.design.routes
  (:require [designvote.middleware :as mw]
            [designvote.design.handlers :as design]
            [designvote.responses :as responses]
            [designvote.design.spec :as design-spec]
            [spec-tools.data-spec :as ds]
            [clojure.spec.alpha :as s]
            [reitit.ring.middleware.multipart :as multipart]))

(defn routes-v2
  [env]
  (let [db (:jdbc-url env)]
    ["/designs" {:swagger {:tags ["designs V2"]}}
     [""
      {:middleware [[mw/wrap-auth0] [mw/wrap-kebab-case]]
       :post       {:handler    (design/create-design-with-versions! db)
                    :parameters {:multipart {:versions    [multipart/temp-file-part]
                                             :name        string?
                                             :designType  string?
                                             :question    string?
                                             :description string?
                                             :voteStyle   string?
                                             :isPublic    boolean?}}
                    :responses  {201 {:body {:designId string?}}}
                    :summary    "Create a design with design versions"}}]]))


(defn routes
  [env]
  (let [db (:jdbc-url env)]
    ["/designs" {:swagger {:tags ["designs"]}}
     [""
      {:middleware [[mw/wrap-auth0]]
       :get        {:handler   (design/list-all-user-designs db)
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
                    :summary    "Create a design"}}]
     ["/latest"
      {:get {:handler    (design/get-latest-designs-paginated db)
             :parameters {:query {(ds/opt :offset) int?
                                  (ds/opt :limit)  int?}}
             :responses  {200 {:body {:designs coll?}}}
             :summary    "Retrieve latest designs paginated. Useful for feed"}}]
     ["/vote/short"
      ["/:short-url"
       {:get {:handler    (design/find-design-by-url db)
              :responses  {200 {:body responses/camelCaseDesign}}
              :parameters {:path {:short-url string?}}
              :summary    "Retrieve design by short url"}}]]

     ["/:design-id"
      [""
       {:middleware [[mw/wrap-auth0] [mw/wrap-authenticated]]
        :get        {:handler    (design/retrieve-design-by-id db)
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
                     :summary    "Publish a design to be ready for voting"}}]


      ["/versions" {:middleware [[mw/wrap-auth0] [mw/wrap-design-owner db] [mw/wrap-kebab-case]]}
       [""
        {:post   {:handler    (design/add-design-version! db)
                  :responses  {201 {:body {:version-id string?}}}
                  :parameters {:path {:design-id string?}
                               :body {:name                 string?
                                      :imageUrl             string?
                                      (ds/opt :description) string?}}
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


      ["/vote" {:middleware [[mw/wrap-auth0] [mw/wrap-authenticated]]}
       ["/rating"
        {:post {:handler    (design/vote-rating-design! db)
                :parameters {:path {:design-id string?}
                             :body {:ratings design-spec/ratings-map}}
                :responses  {201 {:body {:designId string?}}}
                :summary    "Vote on a design with the voting style of 5 star rating"}}]
       ["/choose"
        {:middleware [[mw/wrap-kebab-case]]
         :post       {:handler    (design/vote-choose-best-design! db)
                      :parameters {:path {:design-id uuid?}
                                   :body {:versionId uuid?}}
                      :responses  {201 {:body {:designId uuid?}}}
                      :summary    "Vote on a design with the voting style of choose the best"}}]]
      ["/opinion" {:middleware [[mw/wrap-auth0] [mw/wrap-authenticated] [mw/wrap-kebab-case]]}
       [""
        {:post {:summary    "Add an opinion on a design"
                :handler    (design/add-opinion! db)
                :parameters {:path {:design-id string?}
                             :body {:opinion string?}}
                :responses  {201 {:body map?}
                             500 {:body map?}}}}]
       ["/:opinion-id"
        ["" {:middleware [[mw/wrap-opinion-owner]]}
         {:put    {:summary    "Edit an opinion"
                   :handler    (design/update-opinion! db)
                   :parameters {:path {:design-id  string?
                                       :opinion-id string?}
                                :body {:opinion string?}}}
          :delete {:summary    "Delete an opinion"
                   :handler    (design/delete-opinion! db)
                   :parameters {:path {:design-id  string?
                                       :opinion-id string?}}}}]
        #_["/thumbs-up"
           {:post {:summary    "Upvote an opinion"
                   :handler    (design/upvote-opinion! db)
                   :parameters {:path {:design-id  string?
                                       :opinion-id string?}}}}]]]]]))

