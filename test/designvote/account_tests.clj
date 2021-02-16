(ns designvote.account-tests
  (:require [clojure.test :refer :all]
            [designvote.test-system :as ts]))

(defn account-fixture
  [f]
  (ts/create-auth0-user {:connection "Username-Password-Authentication"
                         :email      "testing@designvote.io"
                         :password   "Sepulcral94"})
  (reset! ts/token (ts/get-test-token "testing@designvote.io"))
  (f)
  (reset! ts/token nil))



(use-fixtures :once account-fixture)

(deftest account-tests
  (testing "Create user account"
    (let [{:keys [status]} (ts/test-endpoint :post "/v1/account/uid" {:auth true})]
      (is (= status 201))))
  (testing "Delete user account"
    (let [{:keys [status]} (ts/test-endpoint :delete "/v1/account" {:auth true})]
      (is (= status 204)))))


(comment

  (ts/get-test-token "testing@designvote.io"))