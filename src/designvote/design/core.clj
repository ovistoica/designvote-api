(ns designvote.design.core
  (:require [designvote.design.db :as db]
            [designvote.account.db :as account-db]
            [designvote.payment.core :as stripe]))

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

