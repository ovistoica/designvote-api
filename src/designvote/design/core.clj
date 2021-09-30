(ns designvote.design.core
  (:require [designvote.design.db :as db]
            [designvote.account.db :as account-db]
            [designvote.media.core :as m]
            [designvote.payment.core :as stripe]
            [clojure.java.io :as io]
            [designvote.media.aws :as aws]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as c]))

(defn generate-short-url
  [design-id]
  (-> design-id (hash/blake2b 3) (c/bytes->hex)))


(defn can-create-design?
  "Check if user either has active subscription
  or has less than two designs on the free trial"
  [db uid]
  (let [count (:count (db/count-user-designs db uid))]
    (if (< count 2)
      true
      (-> (account-db/get-account db uid)
          :stripe-id
          (stripe/active-subscription?)))))

(defn create-design-thumbnail [path1 path2]
  "Generate a design thumbnail from two versions of the poll"
  (let [img1 (m/thumbnail-chunk (m/buffered-image path1))
        img2 (m/thumbnail-chunk (m/buffered-image path2))]
    (m/concat-images img1 img2)))



(defn upload-design-media! [files design-id]
  (let [[img1 img2] (into [] (take 2 files))
        thumbnail (create-design-thumbnail img1 img2)
        img (aws/upload-image! thumbnail (str design-id ".jpeg"))
        v-col (into [] (map-indexed (fn [idx file]
                                      {:name (str (inc idx) ".jpeg")
                                       :img  (m/buffered-image file)}) files))
        version-urls
        (into [] (map #(aws/upload-image! (:img %) (str design-id "-" (:name %))) v-col))]
    {:img-url      img
     :version-urls version-urls}))




(comment

  (mu/buffered-image (io/file "/var/folders/21/917ch7fd5pn68t1qg4nwn4200000gn/T/ring-multipart-2055480968321241375.tmp"))
  (def images [(io/file "resources/image.png") (io/file "resources/image2.png")])
  (create-design-thumbnail images))







