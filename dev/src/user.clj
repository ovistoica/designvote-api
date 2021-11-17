(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [integrant.repl.state :as state]
            [designvote.server]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [ragtime.jdbc :as rjdbc]
            [designvote.account.db :as adb]
            [designvote.design.db.core :as ddb]
            [designvote.design.handlers :as dh]
            [designvote.payment.handlers :as ph]
            [designvote.account.handlers :as h]
            [clojure.java.io :as io]
            [clojure.set :refer [rename-keys]]
            [ragtime.repl :as repl]
            [designvote.payment.core :as p]
            [ring.mock.request :as mock]
            [muuntaja.core :as m]))


(ig-repl/set-prep!
  (fn [] (-> "dev/resources/config.edn" slurp ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def restart ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :designvote/app))
(def db (-> state/system :db/postgres))



; config for db migrations
(def db-url (-> "dev/resources/config.edn" slurp ig/read-string :db/postgres :jdbc-url))
(def config
  {:datastore  (rjdbc/sql-database {:connection-uri db-url})
   :migrations (rjdbc/load-resources "migrations")
   :reporter   println})

(comment
  (repl/migrate config))

(def google-user-id "google-oauth2|117984597083645660112")
(def facebook-user-id "facebook|5841010855939759")
(def mock-design-id "0b423b56-9470-4c5b-9447-88d5d17267a7")
(def mock-ratings {"b8daa0c8-0647-4b5a-8f44-2814656f53bc" 4
                   "f494ba54-9a17-45f6-8759-e2d3b725c6a7" 2})
(def mock-req
  (assoc (mock/request :post (str "/v1/designs/" mock-design-id "/vote/rating"))
    :parameters {:path {:design-id mock-design-id}
                 :body {:ratings mock-ratings}}))

(comment

  (-> (app mock-req)
      (m/decode-response-body))

  ((dh/get-designs-paginated db) {})

  ()

  (def designId "06a0f157-fbc7-42b1-a56e-fe34a3983531")
  (ddb/get-opinions-with-users2 db designId)
  (ddb/find-design-by-id db "b1bbf331-c23f-41ba-ae83-fc74dc1e2478")

  (ddb/update-design-votes-opinions! db designId :total-votes :+)

  (ddb/find-design-by-id db designId)
  (ddb/insert-opinion! db {:design-id designId
                           :opinion   "Asta e aia de increment doi"
                           :uid       google-user-id})


  (ddb/delete-design! db {:design-id "a195d905-d4fb-4892-8c00-a7856ebc8149"})

  (def versions [{:tempfile (io/file "resources/paper_chat1.jpeg")}
                 {:tempfile (io/file "resources/paper_chat2.jpeg")}])

  (def req (assoc (mock/request :post "/v2/designs")
             :claims {:sub "google-oauth2|117984597083645660112"}
             :parameters {:multipart {:versions    versions
                                      :name        "paper-chat"
                                      :question    "Which design do you prefer?"
                                      :design-type "logo"
                                      :vote-style  "choose"
                                      :is-public   true}}))

  (ddb/find-design-by-url db "eeca59")
  (adb/get-public-account db "google-oauth2|110670785301687867257")
  (adb/get-account db "google-oauth2|110670785301687867257")
  (sql/find-by-keys db :account {:uid "google-oauth2|110670785301687867257"})

  (ddb/delete-design! db (select-keys (ddb/find-design-by-url db "5190c2") [:design-id]))
  (ddb/delete-design! db {:design-id "hello2"})
  (ddb/select-latest-designs db)
  (ddb/get-opinion db 126)
  (ddb/delete-opinion! db 126)
  (ddb/update-design! db {:design-id   "54afe9d9-18e4-4cfc-89a6-421525186d36"
                          :total-votes 6})
  (ddb/find-design-by-id db mock-design-id)
  (ddb/insert-vote! db {:design-id  mock-design-id
                        :uid        facebook-user-id
                        :version-id "f494ba54-9a17-45f6-8759-e2d3b725c6a7"})






  (def handler (dh/vote-rating-design! db))
  (handler mock-req))








;; Account testing
(comment
  (adb/get-account db "google-oauth2|117984597083645660112")
  (adb/get-account db "facebook|5841010855939759")
  (adb/get-public-account db "facebook|5841010855939759")


  (adb/update-account! db {:uid "facebook|5841010855939759"} {:subscription-status :trialing})
  (adb/update-account! db {:uid "auth0|6183b1b8fb8291006fc2adae"} {:name "Kaylee Spencer"}))







;; Test functions for payment
(comment

  (p/add-stripe-id-to-user! db (adb/get-account db "facebook|5841010855939759"))

  (def mock-req
    (assoc (mock/request :post "/v1/payment/checkout")
      :claims {:sub "facebook|5841010855939759"}
      :parameters {:body {:success-url "https://designvote.io"
                          :cancel-url  "https://designvote.io"
                          :price-id    "price_1JCNcwIGGMueBEvzdPAkKP47"}}))

  (def db (-> state/system :db/postgres))

  (def handler (create-checkout-session db))

  (handler mock-req))
