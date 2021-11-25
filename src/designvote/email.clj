(ns designvote.email
  (:require [designvote.config :refer [config]]
            [clojure.string :as str])
  (:import (sendinblue.auth ApiKeyAuth)
           (sendinblue Configuration)
           (sibModel CreateContact)
           (java.util Properties)
           (sibApi ContactsApi)))


(def email? (partial re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"))

(defn parse-auth0-contact [{:keys [name email]}]
  ; auth0 uses email as the name if none is provided
  (if (email? name)
    {:email email}
    (let [[first last] (str/split name #" ")]
      {:email      email
       :first-name first
       :last-name  last})))

(defn- set-api-key!
  [email-key]
  (let [client (Configuration/getDefaultApiClient)
        ^ApiKeyAuth api-key (.getAuthentication client "api-key")]
    (doto api-key
      (.setApiKey email-key))))

(defn create-contact-instance [{:keys [email first-name last-name]}]
  (let [attributes (doto (Properties.) (.setProperty "FIRSTNAME" first-name)
                                       (.setProperty "LASTNAME" last-name))]
    (doto (CreateContact.)
      (.setAttributes attributes)
      (.setEmail email)
      (.setListIds [2])
      (.setEmailBlacklisted false)
      (.setSmsBlacklisted false)
      (.setUpdateEnabled false))))



(defn add-contact-to-mailing-list! [auth0-contact]
  (set-api-key! (:email-api-key config))
  (let [api (ContactsApi.)
        contact (-> auth0-contact
                    (parse-auth0-contact)
                    (create-contact-instance))]
    (try
      (.createContact api contact)
      (catch Exception e
        (clojure.pprint/pprint e)))))


