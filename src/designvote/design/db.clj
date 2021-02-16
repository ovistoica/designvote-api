(ns designvote.design.db
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.prepare :as p])
  (:import java.util.UUID))


(defn find-all-designs!
  [db uid]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          public (sql/find-by-keys conn-opts :design {:public true})]
      (if uid
        (let [drafts (sql/find-by-keys conn-opts :design {:public false
                                                          :uid    uid})]
          {:public public
           :drafts drafts})
        {:public public}))))

(defn insert-design!
  [db design]
  (sql/insert! db :design design))

(defn find-design-by-id!
  [db design-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          [design] (sql/find-by-keys conn-opts :design
                                     {:design-id design-id})
          versions (sql/find-by-keys conn-opts :design-version
                                     {:design-id design-id})]
      (if versions (assoc design :versions
                                 (into []
                                       (doall (for [{:keys [version-id] :as version} versions
                                                    :let [pictures
                                                          (sql/find-by-keys conn-opts :picture
                                                                            {:version-id version-id})
                                                          votes (sql/find-by-keys conn-opts :vote
                                                                                  {:version-id version-id})]]
                                                (assoc version :pictures pictures :votes votes)))))
                   design))))


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

(defn construct-db-pictures
  "Extracts pictures uri from the design-version object
  and returns an insertable array of pictures format
  {:picture-id :uri :version-id}"
  [design-version]
  (let [pictures (:pictures design-version)]
    (map #(assoc {:uri %}
            :version-id (:version-id design-version)
            :picture-id (str (UUID/randomUUID))) pictures)))


(defn insert-multiple-design-versions!
  "Given an array of versions containing the
  :picture key as an array of uri's, extracts the
  pictures from every design-version constructs the
  insertable objects for versions and pictures and inserts them in the correct tables"
  [db versions]
  (let [version-cols [:version-id :design-id :name :description]
        pic-cols [:uri :version-id :picture-id]
        db-pictures (into [] (flatten (map construct-db-pictures versions)))]
    (jdbc/with-transaction
      [tx db]
      (let [inserted-versions (sql/insert-multi! tx :design-version version-cols
                                                 (map (apply juxt version-cols) versions)
                                                 (:options db))
            inserted-pics (sql/insert-multi! tx :picture pic-cols
                                             (map (apply juxt pic-cols) db-pictures)
                                             (:options db))]
        (and (pos? (count inserted-versions))
             (pos? (count inserted-pics)))))))

(defn insert-design-version!
  [db version]
  (let [{:keys [pictures]} version
        design-version (dissoc version :pictures)
        db-pictures (into [] (map (fn [pic] [pic (:version-id version)
                                             (str (UUID/randomUUID))]) pictures))]
    (jdbc/with-transaction
      [tx db]
      (let [design-res (sql/insert! tx :design-version design-version (:options db))
            pictures-res (sql/insert-multi! tx :picture
                                            [:uri :version-id :picture-id]
                                            db-pictures (:options db))]
        true))))

(defn update-design-version!
  [db version]
  (-> (sql/update! db :design-version version (select-keys version [:version-id]))
      :next.jdbc/update-count
      (pos?)))

(defn delete-design-version!
  [db design-version]
  (-> (sql/delete! db :design-version design-version)
      :next.jdbc/update-count
      (pos?)))

(defn vote-design-version!
  [db {:keys [version-id design-id] :as data}]
  (jdbc/with-transaction [tx db]
                         (sql/insert! tx :vote (select-keys data [:version-id :opinion :vote-id]) (:options db))
                         (jdbc/execute-one! tx ["UPDATE design
                            SET total_votes = total_votes + 1
                            WHERE design_id = ?" design-id])
                         (jdbc/execute-one! tx ["UPDATE design_version
                            SET votes = votes + 1
                            WHERE version_id = ?" version-id])))

(defn unvote-design-version!
  [db {:keys [version-id design-id vote-id]}]
  (jdbc/with-transaction [tx db]
                         (sql/delete! tx :vote {:vote-id vote-id} (:options db))
                         (jdbc/execute-one! tx ["UPDATE design
                            SET total_votes = total_votes - 1
                            WHERE design_id = ?" design-id])
                         (jdbc/execute-one! tx ["UPDATE design_version
                            SET votes = votes - 1
                            WHERE version_id = ?" version-id])))

(comment
  )
