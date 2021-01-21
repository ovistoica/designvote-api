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
    (let [design-id ((str (UUID/randomUUID)))
          uid (-> request :claims :sub)
          design (-> request :parameters :body)]
      (designs-db/insert-design! db (assoc design :uid uid
                                                  :design-id design-id))
      (rr/created (str responses/base-url "/designs/" design-id)))))


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
        (rr/response 204)
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
        (rr/response 204)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)}))
      )))


(defn add-design-option!
  [db]
  (fn [request]
    (let [option-id (str (UUID/randomUUID))
          design-id (-> request :parameters :path :design-id)
          option (-> request :parameters :body)]
      (designs-db/insert-design-option! db (assoc option :design-id design-id
                                                 :option-id option-id))
      (rr/created (str responses/base-url "/designs/" design-id)))))

(defn update-design-option!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          option (-> request :parameters :body)]
      (designs-db/update-design-option! db (assoc option :design-id design-id))
      (rr/created (str responses/base-url "/designs/" design-id)))))