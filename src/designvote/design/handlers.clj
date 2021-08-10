(ns designvote.design.handlers
  (:require [ring.util.response :as rr]
            [designvote.responses :as responses]
            [designvote.design.db :as designs-db]
            [buddy.core.hash :as hash]
            [clojure.set :refer [rename-keys]]
            [designvote.design.core :as d]
            [buddy.core.codecs :as c])
  (:import java.util.UUID))

;Get all designs from database
(defn list-all-designs!
  [db]
  (fn [request]
    (let [uid (-> request :claims :sub)
          designs (designs-db/find-all-user-designs! db uid)]
      (rr/response designs))))

;Create a design using randomUUID as design-id
(defn create-design!
  [db]
  (fn [request]
    (let [design-id (str (UUID/randomUUID))
          uid (-> request :claims :sub)
          design (-> request :parameters :body)]
      (if (d/can-create-design? db uid)
        (do
          (designs-db/insert-design! db (assoc design :uid uid
                                                      :design-id design-id
                                                      :public false))
          (rr/created (str responses/base-url "/designs/" design-id) {:design-id design-id}))
        {:status 401
         :body {:message "Please upgrade to premium to create more designs"}}))))


(defn retrieve-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          design (designs-db/find-design-by-id! db design-id)]
      (if design
        (rr/response design)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)})))))


(defn delete-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          deleted? (designs-db/delete-design! db {:design-id design-id})]
      (if deleted?
        (rr/status 204)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)})))))


(defn update-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          design-body (-> request :parameters :body)
          ; remove nil values as to not have side effects in db
          to-update (into {} (remove (comp nil? val) design-body))
          updated? (designs-db/update-design! db (assoc to-update :design-id design-id))]
      (if updated?
        (rr/status 204)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)})))))



(defn add-multiple-design-versions
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          req-versions (-> request :parameters :body :versions)
          versions (into [] (map #(assoc % :design-id design-id
                                           :version-id (str (UUID/randomUUID))) req-versions))
          created? (designs-db/insert-multiple-design-versions! db versions)]
      (if created? (rr/created (str responses/base-url "/designs/" design-id)
                               {:design-id design-id})
                   {:status  500
                    :headers {}
                    :body    {:message "Something went wrong. Please try again"}}))))

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
                   (rr/bad-request {:version-id (:version-id version)})))))

(defn delete-design-version!
  [db]
  (fn [request]
    (let [version-id (-> request :parameters :body :version-id)
          deleted? (designs-db/delete-design-version! db {:version-id version-id})]
      (if deleted?
        (rr/status 204)
        (rr/bad-request {:version-id version-id})))))


(defn vote-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          {:keys [voter-id version-id rating vote-style]} (-> request :parameters :body)
          vote-id (str (UUID/randomUUID))]
      (if (= vote-style "five-star")
        (designs-db/rate-vote-design-version! db {:vote-id    vote-id
                                                  :uid        voter-id
                                                  :rating     rating
                                                  :version-id version-id
                                                  :design-id  design-id})
        (designs-db/choose-vote-design-version! db {:vote-id    vote-id
                                                    :uid        voter-id
                                                    :version-id version-id
                                                    :design-id  design-id}))

      (rr/created (str responses/base-url "/designs/" design-id) {:design-id  design-id
                                                                  :version-id version-id
                                                                  :vote-id    vote-id}))))

(defn unvote-design!
  [db]
  (fn [request]
    (let [vote (-> request :parameters :body)
          design-id (-> request :parameters :path :design-id)
          deleted? (designs-db/unvote-design-version! db (assoc vote :design-id design-id))]
      (if deleted?
        (rr/status 204)
        (rr/bad-request {:vote-id (:vote-id vote)})))))


;TODO Retrieve the whole design or at least the short URL
(defn publish-design!
  "Generate public-url for a design and make public true.
  This happens when a user is ready to share his design to voters"
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          short-url (-> design-id (hash/blake2b 3) (c/bytes->hex))
          published? (designs-db/update-design! db {:design-id design-id
                                                    :short-url short-url
                                                    :public    true})]
      (if published? (rr/status 204)
                     (rr/bad-request {:design-id design-id})))))

(defn find-design-by-url
  [db]
  (fn [request]
    (let [short-url (-> request :parameters :path :short-url)]
      (if-let [design (designs-db/find-design-by-url db short-url)]
        (rr/response design)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "short-url" short-url)})))))

(defn add-opinion! [db]
  (fn [{:keys [parameters]}]
    (let [design-id (-> parameters :path :design-id)
          body (:body parameters)
          opinion (-> body
                      (assoc :design-id design-id)
                      (rename-keys {:voter-id :uid}))]
      (if-let [opinion (designs-db/insert-opinion! db opinion)]
        (rr/created (str responses/base-url "/designs/" design-id) opinion)
        (rr/not-found {:type    "design-not-found"
                       :message "design not found"})))))

(defn give-feedback! [db]
  (fn [{:keys [parameters]}]
    (let [body (:body parameters)
          design-id (-> parameters :path :design-id)]
      (designs-db/insert-feedback! db (assoc body :design-id design-id))
      (rr/created (str responses/base-url "/designs/" design-id)))))
