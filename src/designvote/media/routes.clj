(ns designvote.media.routes
  (:require [designvote.middleware :as mw]))

(defn routes
  [env]
  (let [db (:jdbc-url env)]
    ["/media" {:swagger {:tags ["media"]}}
     [""
      {:middleware [[mw/wrap-auth0]]
       :post       {}}]]))
