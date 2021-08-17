(ns designvote.config
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- keywordize [s]
  (-> (str/lower-case s)
      (str/replace "_" "-")
      (str/replace "." "-")
      (keyword)))

(defn- slurp-file [f]
  (when-let [f (io/file f)]
    (when (.exists f)
      (slurp f))))

(defn- sanitize-key [k]
  (let [s (keywordize (name k))]
    (if-not (= k s) (println "Warning: environ key" k "has been corrected to" s))
    s))

(defn- sanitize-val [k v]
  (if (string? v)
    v
    (do (println "Warning: environ value" (pr-str v) "for key" k "has been cast to string")
        (str v))))

(defn- read-env-file [f]
  (when-let [content (slurp-file f)]
    (into {} (for [[k v] (edn/read-string content)]
               [(sanitize-key k) (sanitize-val k v)]))))

(defonce ^{:doc "A map of environment variables."}
         config (read-env-file "resources/secrets.edn"))

