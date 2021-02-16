(ns designvote.responses
  (:require [spec-tools.data-spec :as ds]
            [clojure.spec.alpha :as s]))

(def base-url "https://designvote.io")

(def picture {:uri        string?
              :picture-id string?})

(def vote {:version-id string?
           :opinion   string?})

(def version {:design-id         string?
             :name              string?
             :description       (s/nilable string?)
             (ds/opt :pictures) [picture]
             (ds/opt :votes)    [vote]
             })

(def design
  {:name             string?
   :description      (s/nilable string?)
   :uid              string?
   :public           boolean?
   :total-votes      int?
   (ds/opt :versions) [version]})

(def designs
  {:public          [design]
   (ds/opt :drafts) [design]
   })