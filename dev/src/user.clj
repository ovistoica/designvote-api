(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [integrant.repl.state :as state]
            [designvote.server]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [designvote.design.db :as design-db])
  (:import java.util.UUID))

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

(def option-id (str (UUID/randomUUID)))

(def u-option {:name        "Design option 2"
             :description "Design option description"
             :version-id   "f012da67-4172-4205-8011-31a69527107a"
             :design-id   "6049ca50-ec0f-4bf3-8950-1a3ce1194bec"
             :pictures    ["Picture 1" "Picture 2"]})

;(def db-pictures (into [] (map
;                            (fn [pic] [pic "0d33b525-cf91-4682-aa2c-03944385b922" (str (UUID/randomUUID))])
;                            ["Picture 1" "Picture 2"])))




(comment
  (let [result])
  (println db)
  (jdbc/with-transaction
    [tx db]
    (sql/insert-multi! tx :picture [:uri :option_id :picture_id] db-pictures))
  (println db-pictures)
  (go)
  (halt)
  (reset)

  (design-db/insert-design-version! db u-option)
  (design-db/vote-design-version! db {:design-id "710d10e9-6585-43a4-871b-c4bf532f2313"
                                      :version-id "aa0300a3-2de3-450d-bf08-bccc7dbede84"
                                      :opinion "It is good"})


  (design-db/find-design-by-id! db "710d10e9-6585-43a4-871b-c4bf532f2313")


  ()

  )