(ns designvote.email
  (:require [designvote.config :refer [config]]
            [clojure.string :as str]
            [clj-http.client :as http]
            [designvote.http :refer [handle-response]]
            [clojure.tools.logging :as log]))


(def email-api-base-url "https://api.sendinblue.com/v3/")
(def email-api-key (:email-api-key config))
(def email? (partial re-matches #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"))

(defn handle-error [body]
  (throw (ex-info (:code body) body)))

(defn do-sendinblue-request [request-fn endpoint config]
  "Send api request to SendinBlue"
  (let [url (str email-api-base-url "/" (name endpoint))
        _ (log/trace "Sendinblue API request: %s %s" (pr-str url) (pr-str config))
        request (merge-with merge
                            {:headers          {:api-key email-api-key}
                             :throw-exceptions false
                             :conn-timeout     10000
                             :content-type     :json
                             :accept           :json
                             :socket-timeout   10000}
                            config)]
    (try
      (handle-response (request-fn url request) handle-error)
      (catch Throwable e
        (throw (ex-info (.getMessage e) (merge (ex-data e) {:url url, :request request}) e))))))

(defn- POST!
  "Make a POST request to the Stripe API."
  [endpoint body]
  (do-sendinblue-request http/post endpoint {:form-params body}))


(defn parse-auth0-contact
  "Parse contact to be sent mailing list.
   Auth0 uses email as the name if none is provided"
  [{:keys [name email]}]
  (if (email? name)
    {:email email}
    (let [[first last] (str/split name #" ")]
      {:email      email
       :first-name first
       :last-name  last})))

(defn add-contact-to-mailing-list!
  "Add the new contact to the mailing list"
  [auth0-contact]
  (let [{:keys [first-name last-name email]} (parse-auth0-contact auth0-contact)]

    (try (POST! :contacts {:attributes       {:FIRSTNAME first-name
                                              :LASTNAME  last-name}
                           :listIds          [2]
                           :updateEnabled    false
                           :emailBlacklisted false
                           :smsBlacklisted   false
                           :email            email})
         (catch Exception e
           (clojure.pprint/pprint (ex-data e))))))
