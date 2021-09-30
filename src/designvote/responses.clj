(ns designvote.responses
  (:require [spec-tools.data-spec :as ds]
            [clojure.spec.alpha :as s]
            [designvote.util :as u]))

(def base-url "https://designvote.io")

; TODO Maybe put URI here
(def vote {:version-id string?})

(def version {:design-id      string?
              :name           string?
              :description    (s/nilable string?)
              :image-url      string?
              (ds/opt :votes) [vote]})

(def camelCaseVersion {:designId             string?
                       :name                 string?
                       (ds/opt :description) (s/nilable string?)
                       :imageUrl             string?
                       (ds/opt :votes)       [vote]})


(def opinion {:design-id  string?
              :version-id string?
              :opinion    (s/nilable string?)
              :uid        (s/nilable string?)})

(def camelCaseOpinion {:designId         string?
                       :versionId        string?
                       (ds/opt :opinion) string?
                       (ds/opt :uid)     string?})

(def design
  {:name              string?
   :description       (s/nilable string?)
   :uid               string?
   :is-public         boolean?
   :total-votes       int?
   :short-url         (s/nilable string?)
   :design-type       (s/nilable string?)
   (ds/opt :versions) [version]
   (ds/opt :opinions) [opinion]})

(def camelCaseDesign
  {:name              string?
   :description       (s/nilable string?)
   :uid               string?
   :isPublic          boolean?
   :totalVotes        int?
   :shortUrl          (s/nilable string?)
   :designType        (s/nilable string?)
   (ds/opt :versions) [camelCaseVersion]
   (ds/opt :opinions) [camelCaseOpinion]})

(def camelCaseDesigns [camelCaseDesign])

(def designs [design])
