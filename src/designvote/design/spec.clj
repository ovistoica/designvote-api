(ns designvote.design.spec
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.spec.gen.alpha :as gen]
            [designvote.util :as u]))

(def uuid-regex (re-pattern "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$"))

(defn str-uuid? [input]
  (boolean (re-matches uuid-regex (name input))))


(defn str-uuid-gen []
  (->> (gen/uuid)
       (gen/fmap #(str %))))


(s/def ::design-type #{"logo" "web" "illustration" "mobile" "other"})
(s/def ::name string?)
(s/def ::question string?)


(s/def ::version-id (s/with-gen str-uuid? str-uuid-gen))
(s/def ::rating (s/and pos-int? #(< % 6)))


(def ratings-map
  "Spec for file param created by ring.middleware.multipart-params.temp-file store."
  (st/spec
    {:spec (s/map-of ::version-id ::rating)
     :swagger/type "ratings"}))
