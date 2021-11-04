(ns designvote.util
  (:require [clojure.pprint :refer [pprint]]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [java-time :refer [java-date]])
  (:import (java.io StringWriter)
           (java.util UUID)
           (clojure.lang Keyword)
           (java.time ZoneId)
           (java.net URLDecoder)))


(defn pprint-to-str
  "Returns the output of pretty-printing `x` as a string."
  {:style/indent 1}
  (^String [x]
   (when x
     (with-open [w (StringWriter.)]
       (pprint x w)
       (str w)))))

(defn uuid-str
  "Generates a unique UUID "
  []
  (str (UUID/randomUUID)))

(defn uuid
  []
  (UUID/randomUUID))

(defn ->camelCase [m]
  "Transforms a map to camelCase. Useful for responses for JS frontends"
  (cske/transform-keys csk/->camelCase m))

(defn ->kebab-case [m]
  "Transforms a map to snake-case. Useful for responses from JS frontends"
  (cske/transform-keys csk/->kebab-case m))

(defn keyword->sql-text
  "Transform a keyword to be stored in DB"
  [^Keyword kw]
  (-> kw
      (csk/->snake_case)
      (name)))

(defn sql-timestamp->inst [t]
  (-> t
      (.toLocalDateTime)
      (.atZone (ZoneId/systemDefault))
      (java-date)))

(defn decode-uri
  [uri]
  (URLDecoder/decode uri))
