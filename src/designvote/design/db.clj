(ns designvote.design.db
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc])
  (:import java.util.UUID))


(defn find-all-user-designs!
  [db uid]
  (let [designs (sql/find-by-keys db :design {:uid uid})]
    designs))

(defn insert-design!
  [db design]
  (sql/insert! db :design design))

(defn build-versions
  "Given a db-connection and an array of design versions,
  this function will populate the versions with their respective
  pictures and votes"
  [conn versions]
  (into []
        (doall
          (for [{:keys [version-id] :as version} versions
                :let [
                      query {:version-id version-id}
                      pictures (sql/find-by-keys conn :picture query)
                      votes (sql/find-by-keys conn :vote query)
                      opinions (sql/find-by-keys conn :opinion query)]]
            (assoc version :pictures pictures :votes votes :opinions opinions)))))

(defn find-design-by-id!
  [db design-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          [design] (sql/find-by-keys conn-opts :design
                                     {:design-id design-id})]
      (if design (let [query (select-keys design [:design-id])
                       versions (sql/find-by-keys conn-opts :design-version query)
                       opinions (sql/find-by-keys conn-opts :opinion query)]
                   (if (not (empty? versions))
                     (assoc design :opinions opinions
                                   :versions (build-versions conn-opts versions))
                     design))
                 nil))))


(defn update-design!
  [db design]
  (-> (sql/update! db :design design (select-keys design [:design-id]))
      :next.jdbc/update-count
      (pos?)))

(defn delete-design!
  [db design]
  ;TODO DELETE DESIGN VERSIONS VOTES AND OPINIONS
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

;TODO ensure correct verification of insertion
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

(def not-nil? (complement nil?))


(defn choose-vote-design-version!
  "Vote on a design version. If the user has
  already voted on that version, update his
  vote and don't make a new entry in db"
  [db {:keys [version-id design-id uid] :as data}]
  (let [vote-query {:design-id design-id :uid uid}
        db-opts (:options db)
        new-vote (select-keys data [:version-id :uid :vote-id :design-id])]
    (jdbc/with-transaction [tx db]
                           (if-let [existent-vote (not-empty (sql/find-by-keys tx :vote vote-query db-opts))]
                             (sql/update! tx :vote {:version-id version-id} vote-query db-opts)
                             (do
                               (sql/insert! tx :vote new-vote db-opts)
                               (jdbc/execute-one! tx ["UPDATE design
                            SET total_votes = total_votes + 1
                            WHERE design_id = ?" design-id])
                               (jdbc/execute-one! tx ["UPDATE design_version
                            SET votes = votes + 1
                            WHERE version_id = ?" version-id]))))))

(defn rate-vote-design-version!
  "Vote on a design version. If the user has
  already voted on that version, update his
  vote and don't make a new entry in db"
  [db {:keys [version-id design-id uid rating] :as data}]
  (let [vote-keys {:version-id version-id :uid uid}
        db-opts (:options db)
        new-vote (select-keys data [:version-id :uid :rating :vote-id])]
    (jdbc/with-transaction [tx db]
                           (if-let [existent-vote (-> (sql/find-by-keys tx :vote vote-keys db-opts)
                                                      not-empty)]
                             (sql/update! tx :vote {:rating rating} vote-keys db-opts)
                             (do
                               (sql/insert! tx :vote new-vote db-opts)
                               (jdbc/execute-one! tx ["UPDATE design
                            SET total_votes = total_votes + 1
                            WHERE design_id = ?" design-id])
                               (jdbc/execute-one! tx ["UPDATE design_version
                            SET votes = votes + 1
                            WHERE version_id = ?" version-id]))))))

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


(defn find-design-by-url
  [db short-url]
  (with-open [conn (jdbc/get-connection db)]

    (let [conn-opts (jdbc/with-options conn (:options db))
          [design] (sql/find-by-keys conn-opts :design
                                     {:short-url short-url})]
      (if design
        (let [query (select-keys design [:design-id])
              versions (sql/find-by-keys conn-opts :design-version query)
              opinions (sql/find-by-keys conn-opts :opinion query)]
          (if (not (empty? versions))
            (assoc design :opinions opinions
                          :versions (build-versions conn-opts versions))
            design))
        nil))))

(defn insert-opinion!
  "Insert an opinion in db. Necessary keys on opinion map:
   `:design-id` - FK for id of design
   `:version-id` - FK for id of version
   `:opinion` - The actual opinion text "
  [db opinion-map]
  (sql/insert! db :opinion opinion-map))


