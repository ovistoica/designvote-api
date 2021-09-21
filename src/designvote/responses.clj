(ns designvote.responses
  (:require [spec-tools.data-spec :as ds]
            [clojure.spec.alpha :as s]))

(def base-url "https://designvote.io")

(def picture {:uri        string?
              :picture-id string?})

(def vote {:version-id string?})

(def version {:design-id         string?
              :name              string?
              :description       (s/nilable string?)
              (ds/opt :pictures) [picture]
              (ds/opt :votes)    [vote]})


(def opinion {:design-id  string?
              :version-id string?
              :opinion    (s/nilable string?)
              :uid        (s/nilable string?)})

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

(def designs [design])
