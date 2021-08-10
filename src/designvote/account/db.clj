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
    (dissoc user :password)))

(defn update-account!
  "Update values from an account entry
  Example:
  (update-user! \"123-123-123\" {:password \"new-password\"}"
  [db uid values]
  (let [where-map {:uid uid}]
    (sql/update! db :account values where-map)))
