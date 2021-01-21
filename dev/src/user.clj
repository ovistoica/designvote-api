(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [integrant.repl.state :as state]
            [designvote.server]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(ig-repl/set-prep!
  (fn [] (-> "dev/resources/config.edn" slurp ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :designvote/app))
(def db (-> state/system :db/postgres))

(def design
  {:design-id   "a1995316-80ea-4a98-939d-7c6295e4bb46",
   :name        "Feed",
   :public      true,
   :img         "https://res.cloudinary.com/stoica94/image/upload/v1611124828/samples/ecommerce/car-interior-design.jpg",
   :description nil,
   :total-votes 4,
   :uid         "jade@mailinator.com",
   })

(def options
  [{:option-id   "22a82a84-91cc-40e2-8775-d5bee9d188ff",
    :name        "Feed 1",
    :description "With cool title",
    :design-id   "a1995316-80ea-4a98-939d-7c6295e4bb46",
    :votes       2}
   {:option-id   "64f0aed2-157e-481a-a318-8752709e5a5a",
    :name        "Feed 2",
    :description "Without cool title",
    :design-id   "a1995316-80ea-4a98-939d-7c6295e4bb46",
    :votes       2}])

(def pictures [{:picture-id "05cbe0ef-fd8a-47a0-8602-2c154a06edba",
                :uri        "https://res.cloudinary.com/stoica94/image/upload/v1611124826/samples/people/jazz.jpg",
                :option-id  "64f0aed2-157e-481a-a318-8752709e5a5a"}
               {:picture-id "aaa7ab14-efd7-45a1-ac86-aa6bfe13a2ab",
                :uri        "https://res.cloudinary.com/stoica94/image/upload/v1611124829/samples/ecommerce/leather-bag-gray.jpg",
                :option-id  "22a82a84-91cc-40e2-8775-d5bee9d188ff"}])


(comment
  (let [result ])
  (println db)

  (sql/query db ["SELECT * FROM design_option a
                  LEFT OUTER JOIN picture b ON a.option_id = a.option_id
                  WHERE a.design_id = ?"
                 "a1995316-80ea-4a98-939d-7c6295e4bb46"])
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          [design] (sql/find-by-keys conn-opts :design
                                     {:design-id "a1995316-80ea-4a98-939d-7c6295e4bb46"})
          options (sql/find-by-keys conn-opts :design_option
                                    {:design-id "a1995316-80ea-4a98-939d-7c6295e4bb46"})]
      (assoc design :options (doall (for [{:design-option/keys [option-id & rest] :as option} options
                   :let [pictures (sql/find-by-keys conn-opts :picture
                                                    {:option-id option-id})]]
               (assoc rest :option-id option-id :pictures pictures))))))
  (go)
  (halt)
  (reset)
  (let [conversations (sql/find-by-keys db :conversation {:uid "auth0|5ef440986e8fbb001355fd9c"})]
    (for [{:conversation/keys [conversation-id]} conversations
          :let [created-at (sql/query db ["SELECT created_at FROM message
                                            WHERE conversation_id = ?
                                            ORDER BY created_at DESC
                                            LIMIT 1" conversation-id])]]))


  (sql/find-by-keys db :conversation {:uid "auth0|5ef440986e8fbb001355fd9c"})
  (jdbc/execute-one! db ["SELECT created_at FROM message
                                              WHERE conversation_id = ?
                                              ORDER BY created_at DESC
                                              LIMIT 1" "8d4ab926-d5cc-483d-9af0-19627ed468eb"])

  (jdbc/execute-one! db ["SELECT uid FROM conversation
                                                   WHERE uid != ? AND conversation_id = ?"
                         "auth0|5ef440986e8fbb001355fd9c" "8d4ab926-d5cc-483d-9af0-19627ed468eb"])


  (with-open [conn (jdbc/get-connection db)]
    (let [conn-options (jdbc/with-options conn (:options db))
          conversations (sql/find-by-keys conn :conversation {:uid "auth0|5ef440986e8fbb001355fd9c"})]
      (doall
        (for [{:conversation/keys [conversation-id] :as conversation} conversations
              :let [{:message/keys [created-at]}
                    (jdbc/execute-one! conn-options ["SELECT created_at FROM message
                                              WHERE conversation_id = ?
                                              ORDER BY created_at DESC
                                              LIMIT 1" conversation-id])
                    with (jdbc/execute-one! conn-options ["SELECT uid FROM conversation
                                                   WHERE uid != ? AND conversation_id = ?" "auth0|5ef440986e8fbb001355fd9c" conversation-id])
                    [{:account/keys [name picture]}] (sql/find-by-keys conn-options :account with)]]
          (assoc conversation :conversation/updated-at created-at
                              :with-name name
                              :with-picture picture)))))




  (let [[design] (sql/find-by-keys db :design {:design-id "a1995316-80ea-4a98-939d-7c6295e4bb46"})]
    design))