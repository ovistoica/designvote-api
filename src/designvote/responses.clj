(ns designvote.responses
  (:require [spec-tools.data-spec :as ds]
            [clojure.spec.alpha :as s]))

(def base-url "https://designvote.io")

(def picture {:picture/uri        string?
              :picture/picture-id string?})

(def vote {:vote/option-id string?
           :vote/opinion   string?})

(def option {:option/design-id         string?
             :option/name              string?
             :option/description       string?
             (ds/opt :option/pictures) [picture]
             (ds/opt :option/votes)    [vote]
             })

(def design
  {:design/name             string?
   :design/description      (s/nilable string?)
   :design/uid              string?
   :design/public           boolean?
   :design/total-votes      int?
   (ds/opt :design/options) [option]})

(def designs
  {:public          [design]
   (ds/opt :drafts) [design]
   })