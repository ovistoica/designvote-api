(ns designvote.account.db
  (:require [next.jdbc.sql :as sql]))

(defn create-account!
  [db account]
  (sql/insert! db :account account))

(defn delete-account!
  [db account]
  (sql/delete! db :account account))

(defn get-account
  [db uid]
  (let [[user] (sql/find-by-keys db :account {:uid uid})]
    (-> user
        (dissoc :password)
        (assoc :subscription-status (keyword (:subscription-status user))))))

(defn update-account!
  "Update values from an account entry
  Example:
  (update-user! {:uid \"123-123-123\"} {:password \"new-password\"}"
  [db where-map values]
  (sql/update! db :account values where-map))
