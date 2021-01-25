(ns designvote.design.handlers
  (:require [ring.util.response :as rr]
            [designvote.responses :as responses]
            [designvote.design.db :as designs-db])
  (:import java.util.UUID))

;Get all designs from database
(defn list-all-designs!
  [db]
  (fn [request]
    (let [uid (-> request :claims :sub)
          designs (designs-db/find-all-designs! db uid)]
      (rr/response designs))))

;Create a design using randomUUID as design-id
(defn create-design!
  [db]
  (fn [request]
    (let [design-id (str (UUID/randomUUID))
          uid (-> request :claims :sub)
          design (-> request :parameters :body)]
      (designs-db/insert-design! db (assoc design :uid uid
                                                  :design-id design-id
                                                  :public false))
      (rr/created (str responses/base-url "/designs/" design-id) {:design-id design-id}))))


(defn retrieve-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          design (designs-db/find-design-by-id! db design-id)]
      (if design
        (rr/response design)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)}))
      )))

(defn delete-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          deleted? (designs-db/delete-design! db {:design-id design-id})]
      (if deleted?
        (rr/status 204)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)}))
      )))

(defn update-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          design (-> request :parameters :body)
          updated? (designs-db/update-design! db (assoc design :design-id design-id))]
      (if updated?
        (rr/status 204)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)}))
      )))


(defn add-design-version!
  [db]
  (fn [request]
    (let [version-id (str (UUID/randomUUID))
          design-id (-> request :parameters :path :design-id)
          version (-> request :parameters :body)
          created? (designs-db/insert-design-version! db (assoc version :design-id design-id
                                                                      :version-id version-id))]
      (if created? (rr/created (str responses/base-url "/designs/" design-id) {:version-id version-id})
                   {:status  500
                    :headers {}
                    :body    {:message "Something went wrong. Please try again"}}))))

(defn update-design-version!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          version (-> request :parameters :body)
      updated? (designs-db/update-design-version! db (assoc version :design-id design-id))]
      (if updated? (rr/status 204)
                   (rr/not-found {:type    "version-not-found"
                                  :message "Design version not found"
                                  :data    (str "version-id" (:version-id version))})  ))))