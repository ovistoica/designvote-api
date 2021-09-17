(ns designvote.design.core
  (:require [designvote.design.db :as db]
            [designvote.account.db :as account-db]
            [designvote.media.util :as mu]
            [designvote.media.core :as m]
            [designvote.payment.core :as stripe]
            [clojure.java.io :as io]
            [clojure.string :as string]))

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
  (let [img1 (m/thumbnail (m/buffered-image path1))
        img2 (m/thumbnail (m/buffered-image path2))]
    (-> (m/concat-images img1 img2)
        (m/write-image "resources/final.jpeg"))))


(comment

  (mu/buffered-image (io/file "/var/folders/21/917ch7fd5pn68t1qg4nwn4200000gn/T/ring-multipart-2055480968321241375.tmp"))
  (def images [(io/file "resources/image.png") (io/file "resources/image2.png")])
  (create-design-thumbnail images))







