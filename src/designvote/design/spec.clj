(ns designvote.design.spec
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]))


(s/def ::design-type #{"logo" "web" "illustration" "mobile" "other"})
(s/def ::name string?)
(s/def ::question string?)


