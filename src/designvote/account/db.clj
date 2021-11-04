(ns designvote.account.db
  (:refer-clojure :exclude [filter group-by partition-by set update set])
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [honey.sql.helpers :refer [select where from]]
            [honey.sql :as h]))

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

(defn get-public-account
  "Retrieve public info about an account. Returns nil if no account was found"
  [db uid]
  (first (jdbc/execute! db (-> (select :name :uid :picture :nickname)
                               (from :account)
                               (where [:= :uid uid])
                               (h/format)))))

