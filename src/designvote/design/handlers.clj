(ns designvote.design.handlers
  (:require [ring.util.response :as rr]
            [designvote.responses :as responses]
            [designvote.design.db.core :as db]
            [clojure.set :refer [rename-keys]]
            [designvote.design.core :as d]
            [designvote.util :as u])
  (:import java.util.UUID))


(defn get-designs-paginated
  "Get design polls paginated."
  [db]
  (fn [req]
    (let [{:keys [limit offset]} (-> req :parameters :query)
          popular (-> (db/select-most-popular-designs db {:limit  limit
                                                          :offset offset}))

          latest (db/select-latest-designs db {:limit  limit
                                               :offset offset})]
      (rr/response (u/->camelCase {:latest  latest
                                   :popular popular})))))



(defn list-all-user-designs
  "Get all designs for the user from database"
  [db]
  (fn [request]
    (let [uid (-> request :claims :sub)
          designs (db/find-all-user-designs db uid)]
      (rr/response designs))))

(defn create-design!
  "Create a design using randomUUID as design-id "
  [db]
  (fn [request]
    (let [design-id (str (UUID/randomUUID))
          uid (-> request :claims :sub)
          design (-> request :parameters :body)]
      (if (d/can-create-design? db uid)
        (do
          (db/insert-design! db (assoc design :uid uid
                                              :design-id design-id
                                              :public false))
          (rr/created (str responses/base-url "/designs/" design-id) {:design-id design-id}))
        {:status 401
         :body   {:message "Please upgrade to premium to create more designs"}}))))


(defn retrieve-design-by-id
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          design (db/find-design-by-id db design-id)]
      (if design
        (rr/response design)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)})))))


(defn delete-design!
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          deleted? (db/delete-design! db {:design-id design-id})]
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
          updated? (db/update-design! db (assoc to-update :design-id design-id))]
      (if updated?
        (rr/status 204)
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "design-id" design-id)})))))



(defn create-design-with-versions! [db]
  "Insert a new design along with its versions in the DB.

   The version's images are uploaded to DO Spaces along with the
   newly created thumbnail. Urls to the new images are stored in DB."
  (fn [req]
    (let [mp (-> req :parameters :multipart)
          design (dissoc mp :versions)
          uid (-> req :claims :sub)
          v-files (map :tempfile (:versions mp))
          design-id (u/uuid-str)
          short-url (d/generate-short-url design-id)
          {:keys [img-url version-urls]} (d/upload-design-media! v-files design-id)
          extra-keys {:design-id design-id
                      :uid       uid
                      :img       img-url
                      :short-url short-url}

          created? (db/insert-full-design! db (merge design extra-keys) version-urls)]
      (if created?
        (rr/created (str responses/base-url "/designs/" design-id)
                    {:designId design-id
                     :shortUrl short-url})
        {:status 500
         :body   {:message "Something went wrong. Please try again"}}))))



(defn add-design-version!
  [db]
  (fn [request]
    (let [version-id (u/uuid-str)
          design-id (-> request :parameters :path :design-id)
          version (-> request :parameters :body)
          created? (db/insert-design-version! db (assoc version :design-id design-id
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
          updated? (db/update-design-version! db (assoc version :design-id design-id))]
      (if updated? (rr/status 204)
                   (rr/bad-request {:version-id (:version-id version)})))))

(defn delete-design-version!
  [db]
  (fn [request]
    (let [version-id (-> request :parameters :body :version-id)
          deleted? (db/delete-design-version! db {:version-id version-id})]
      (if deleted?
        (rr/status 204)
        (rr/bad-request {:version-id version-id})))))


(defn publish-design!
  "Generate public-url for a design and make public true.
  This happens when a user is ready to share his design to voters"
  [db]
  (fn [request]
    (let [design-id (-> request :parameters :path :design-id)
          short-url (d/generate-short-url design-id)
          published? (db/update-design! db {:design-id design-id
                                            :short-url short-url
                                            :public    true})]
      (if published? (rr/status 204)
                     (rr/bad-request {:design-id design-id})))))

(defn find-design-by-url
  "Retrieve design by public short-url. Used for accessing design voting page"
  [db]
  (fn [request]
    (let [short-url (-> request :parameters :path :short-url)]
      (if-let [design (db/find-design-by-url db short-url)]
        (do

          (rr/response (u/->camelCase design)))
        (rr/not-found {:type    "design-not-found"
                       :message "Design not found"
                       :data    (str "short-url" short-url)})))))

(defn add-opinion! [db]
  "Add an opinion on a design"
  (fn [{:keys [parameters claims]}]
    (let [uid (-> claims :sub)
          design-id (-> parameters :path :design-id)
          body (:body parameters)
          opinion (-> body
                      (assoc :design-id design-id
                             :uid uid))]
      (if-let [opinion (db/insert-opinion! db opinion)]
        (rr/created (str responses/base-url "/designs/" design-id) opinion)
        (rr/not-found {:type    "design-not-found"
                       :message "design not found"})))))

(defn update-opinion! [db]
  (fn [{:keys [parameters]}]
    (let [opinion-id (-> parameters :path :opinion-id)
          opinion (-> parameters :body :opinion)
          updated? (db/update-opinion! db opinion opinion-id)]
      (if updated?
        (rr/response {:message "Updated opinion"})
        (rr/not-found {:type    :opinion-not-found
                       :message "Opinion not found!"})))))

(defn delete-opinion! [db]
  (fn [{:keys [parameters]}]
    (let [design-id (-> parameters :path :design-id)
          opinion-id (-> parameters :path :opinion-id)
          deleted? (db/delete-opinion! db design-id opinion-id)]
      (if deleted?
        (rr/response {:message "Deleted opinion"})
        (rr/not-found {:type    :opinion-not-found
                       :message "Opinion not found!"})))))

;(defn upvote-opinion! [db]
;  (fn [{:keys [parameters]}]
;    (let [opinion-id (-> parameters :path :opinion-id)
;          upvoted? (db/upvote-opinion! db opinion-id)]
;      (if upvoted?
;        (rr/response {:message "Success"})
;        (rr/not-found {:type    :opinion-not-found
;                       :message "Opinion not found!"})))))

(defn vote-rating-design! [db]
  "Vote on a design with the voting style of 5 star rating"
  (fn [req]
    (let [uid (-> req :claims :sub)
          design-id (-> req :parameters :path :design-id)
          ratings (-> req :parameters :body :ratings)
          inserted? (db/insert-ratings! db {:design-id design-id
                                            :uid       uid
                                            :ratings   ratings})]
      (if inserted?
        (rr/created (str responses/base-url "/designs/" design-id) {:designId design-id})
        {:status 500
         :body   {:message "Something went wrong. Please try again"}}))))

(defn vote-choose-best-design! [db]
  "Vote on a design with the voting style of choose the best"
  (fn [req]
    (let [uid (-> req :claims :sub)
          design-id (-> req :parameters :path :design-id)
          version-id (-> req :parameters :body :version-id)
          inserted? (db/insert-vote! db {:design-id  design-id
                                         :uid        uid
                                         :version-id version-id})]
      (if inserted?
        (rr/created (str responses/base-url "/designs/" design-id) {:designId design-id})
        {:status 500
         :body   {:message "Something went wrong. Please try again"}}))))
