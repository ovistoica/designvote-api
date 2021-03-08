(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [integrant.repl.state :as state]
            [designvote.server]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [designvote.design.db :as design-db]
            [buddy.core.hash :as h]
            [buddy.core.codecs :as c])

  (:import java.util.UUID))

(ig-repl/set-prep!
  (fn [] (-> "dev/resources/config.edn" slurp ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :designvote/app))
(def db (-> state/system :db/postgres))
(println db)

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
  [{:name        "Feed 1",
    :description "With cool title",
    :pictures    ["picture1" "picture2"]}
   {:name        "Feed 2",
    :description "Without cool title",
    :pictures    ["otherpicture", "otherother picture"]
    }])

(def option1 (first options))

(def pics (:pictures option1))

(defn construct-pictures [option]
  (let [pictures (:pictures option)]
    (map #(assoc {:uri %}
            :option-id (:option-id option)
            :picture-id (str (UUID/randomUUID))) pictures)))

(def design-id "a1995316-80ea-4a98-939d-7c6295e4bb46")
(def db-opts (map #(dissoc % :pictures) options))



(def finalPicts (map (fn [elem] (assoc {:uri elem}
                                  :option-id (:option-id option1)
                                  :picture-id (str (UUID/randomUUID)))) pics))



;(def u-option {:name        "Design option 2"
;               :description "Design option description"
;               :version-id  "f012da67-4172-4205-8011-31a69527107a"
;               :design-id   "6049ca50-ec0f-4bf3-8950-1a3ce1194bec"
;               :pictures    ["Picture 1" "Picture 2"]})
;
;(def db-pictures (into [] (map
;                            (fn [pic] [pic "0d33b525-cf91-4682-aa2c-03944385b922" (str (UUID/randomUUID))])
;                            ["Picture 1" "Picture 2"])))


(def db-versions
  [{:description "Design option description"
    :name        "Design option 1"
    :design-id   "9cd035eb-a4d0-4ce2-ae75-b3b3cdcb76ed"
    :version-id  "82b6640d-5c4b-4ddd-aae6-4345ead643e4"}
   {:description "Design option description"
    :name        "Design option 2"
    :design-id   "9cd035eb-a4d0-4ce2-ae75-b3b3cdcb76ed"
    :version-id  "4e901694-4279-4722-bfea-8fd841f772aa"}])

(defn parse-pics
  []
  (let [pics (map :pictures options)] pics))

(defn orderd
  [version]
  (let [{:keys [design-id version-id name description]} version]
    [design-id version-id name description]))



(comment
  (into [] (flatten (map construct-db-pictures options)))
  (let [result])
  (println db)
  (orderd (first db-versions))
  (map #(let [{:keys [design-id version-id name description]} %]
          [design-id version-id name description]) db-versions)


  (sql/insert-multi! db :design-version [:description :name :design-id :version-id]
                     db-versions)
  (jdbc/with-transaction
    [tx db]
    (sql/insert-multi! tx :picture [:uri :option_id :picture_id] db-pictures))
  (println db-pictures)
  (go)
  (halt)
  (reset)
  (parse-pics)
  (->"710d10e9-6585-43a4-871b-c4bf532f2311" (h/blake2b  3) (c/bytes->hex))

  (design-db/insert-design-version! db u-option)
  (design-db/vote-design-version! db {:design-id  "710d10e9-6585-43a4-871b-c4bf532f2313"
                                      :version-id "aa0300a3-2de3-450d-bf08-bccc7dbede84"
                                      :opinion    "It is good"})


  (design-db/find-design-by-id! db "710d10e9-6585-43a4-871b-c4bf532f2313")



  )