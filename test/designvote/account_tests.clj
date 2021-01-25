(ns designvote.account-tests
  (:require [clojure.test :refer :all]
            [designvote.test-system :as ts]))

(defn account-fixture
  [f]
  (ts/create-auth0-user {:connection "Username-Password-Authentication"
                         :email      "account-testing@designvote.io"
                         :password   "Sepulcral94"})
  (reset! ts/token (ts/get-test-token "account-testing@designvote.io"))
  (f)
  (reset! ts/token nil))



(use-fixtures :once account-fixture)

(deftest account-tests
  (testing "Create user account"
    (let [{:keys [status]} (ts/test-endpoint :post "/v1/account" {:auth true})]
      (is (= status 201))))
  (testing "Delete user account"
    (let [{:keys [status]} (ts/test-endpoint :delete "/v1/account" {:auth true})]
      (is (= status 204)))))

(defn salut-beatrice
  [adjectiv]
  (println "Hei Beatrice esti sexy! :>")
  (println (str "My ass is " adjectiv)))

(comment
  (salut-beatrice "Sexy and fuckable")

  (account-fixture [(fn [] "hello")])

         (ts/get-test-token "account-testing@designvote.io"))