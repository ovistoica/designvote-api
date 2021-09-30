(ns designvote.design.db
  (:refer-clojure :exclude [filter group-by partition-by set update])
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [honey.sql.helpers :refer [select where from order-by offset limit]]
            [honey.sql :as h]
            [designvote.util :as u]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Design queries ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-all-user-designs
  [db uid]
  (sql/find-by-keys db :design {:uid uid}))


(defn insert-design!
  [db design-map]
  (sql/insert! db :design design-map))

(defn update-design!
  [db design]
  (-> (sql/update! db :design design (select-keys design [:design-id]))
      :next.jdbc/update-count
      (pos?)))

(defn delete-design!
  [db design]
  (-> (sql/delete! db :design design)
      :next.jdbc/update-count
      (pos?)))


(defn find-design-by-id
  [db design-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          query {:design-id design-id}
          [design] (sql/find-by-keys conn-opts :design query)]
      (when design
        (let [versions (sql/find-by-keys conn-opts :design-version query)
              opinions (sql/find-by-keys conn-opts :opinion query)
              votes (sql/find-by-keys conn-opts :vote query)]
          (assoc design :opinions opinions
                        :versions versions
                        :votes votes))))))

(defn find-design-by-url
  [db short-url]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          [design] (sql/find-by-keys conn-opts :design
                                     {:short-url short-url})]
      (when design
        (let [owner (jdbc/execute-one! conn-opts
                                       ["SELECT name, picture, nickname FROM account WHERE
                                                      uid = ? " (:uid design)])
              query (select-keys design [:design-id])
              versions (sql/find-by-keys conn-opts :design-version query)
              opinions (sql/find-by-keys conn-opts :opinion query)
              votes (sql/find-by-keys conn-opts :vote query)]
          (if (not (empty? versions))
            (assoc design :owner owner
                          :opinions opinions
                          :versions versions
                          :votes votes)
            design))))))

(defn select-latest-designs
  "Retrieves the most recent created public polls"
  ([db]
   (select-latest-designs db {}))
  ([db {:keys [offset-by limit-by] :or {offset-by 0 limit-by 10}}]
   (jdbc/execute! db (-> (select :*)
                         (from :design)
                         (order-by [:created-at :desc])
                         (offset offset-by)
                         (limit limit-by)
                         (h/format)))))

(defn count-user-designs
  "Get number of designs created by the user.
  Useful for trial periods of users"
  [db uid]
  (jdbc/execute-one! db (-> (select :%count.*)
                            (from :design)
                            (where [:= :uid uid])
                            (h/format))))

(defn insert-design-version!
  [db version-map]
  (sql/insert! db :design-version version-map))

(defn- insert-multiple-design-versions!
  ([db design-id images-url-col]
   (insert-multiple-design-versions! db design-id images-url-col {}))
  ([db design-id images-url-col opts]
   (let [versions
         (map-indexed (fn [idx url] {:name       (str "#" (inc idx))
                                     :image-url  url
                                     :version-id (u/uuid-str)
                                     :design-id  design-id}) images-url-col)
         v-cols (-> versions (first) (keys) (vec))]
     (sql/insert-multi! db :design-version v-cols (map #(vec (vals %)) versions) opts))))


(defn insert-full-design! [db design version-urls]
  (let [design-id (:design-id design)
        opts (:options db)]
    (jdbc/with-transaction [tx db]
      (let [inserted-design (sql/insert! tx :design design opts)
            inserted-versions (insert-multiple-design-versions! tx design-id version-urls opts)]
        (and inserted-design
             (pos? (count inserted-versions)))))))


(defn update-design-version!
  [db updated-map]
  (-> (sql/update! db :design-version updated-map (select-keys updated-map [:version-id]))
      :next.jdbc/update-count
      (pos?)))

(defn delete-design-version!
  [db design-version]
  (-> (sql/delete! db :design-version design-version)
      :next.jdbc/update-count
      (pos?)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Votes, Ratings and Opinions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn insert-opinion!
  "Insert an opinion in db. Necessary keys on opinion map:

   :design-id - Id of the design
   :opinion   - The actual opinion text
   :uid       - The id of the user who added the opinion"
  [db opinion-map]
  (sql/insert! db :opinion opinion-map))


(defn- build-ratings [ratings design-id uid]
  "Normalize kv ratings to insert in db"
  (into [] (for [[k v] ratings] {:version-id (name k)
                                 :rating     v
                                 :uid        uid
                                 :design-id  design-id})))

(defn insert-ratings! [db {:keys [design-id ratings uid]}]
  "Insert 5-star ratings for a design.

   Parameters:
   :design-id     The id of the design to insert ratings for
   :uid           The id of the user who voted
   :rating        A map with the version id as a key and a rating (1-5) as value"
  (let [db-ratings (build-ratings ratings design-id uid)
        rating-cols [:version-id :design-id :uid :rating]]
    (-> (sql/insert-multi! db :vote rating-cols
                           (map (apply juxt rating-cols) db-ratings) (:options db))
        (count)
        (pos?))))

(defn insert-vote!
  "Insert choose-best vote for a design.

   Parameters inside the vote map:
   :design-id     The id of the design to insert ratings for
   :uid           The id of the user who voted
   :version-id    The id of the version which was chosen"
  [db vote-map]
  (sql/insert! db :vote vote-map))
