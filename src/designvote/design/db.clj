(ns designvote.design.db
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]))


(defn find-all-designs!
  [db uid]
  (with-open [conn (jdbc/get-connection db)]
    (let [public (sql/find-by-keys conn :design {:public true})]
      (if uid
        (let [drafts (sql/find-by-keys conn :design {:public false
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
          options (sql/find-by-keys conn-opts :design_option
                                    {:design-id design-id})]
      (assoc design :options
                    (doall (for [{:design-option/keys [option-id & rest] :as option} options
                                 :let [pictures
                                       (sql/find-by-keys conn-opts
                                                         :picture
                                                         {:option-id option-id})]]
                             (assoc rest :option-id option-id :pictures pictures)))))))


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

(defn insert-design-option!
  [db [{:keys [pictures & option]}]]
  option
  )

(defn update-design-option!
  [db [{:keys [pictures & option]}]]
  option
  )

(comment
  )
