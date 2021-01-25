(ns designvote.design-tests
  (:require [clojure.test :refer :all]
            [designvote.server :refer :all]
            [designvote.test-system :as ts]
            ))

(defn token-fixture
  "Fixture to create a new user with auth0, retrieve correct token for tests and delete user at cleanup"
  [f]
  (ts/create-auth0-user {:connection "Username-Password-Authentication"
                         :email      "account-testing@designvote.io"
                         :password   "Sepulcral94"})
  (reset! ts/token (ts/get-test-token "account-testing@designvote.io"))
  (ts/test-endpoint :post "/v1/account" {:auth true})
  (f)
 #_(ts/test-endpoint :delete "/v1/account" {:auth true})
  (reset! ts/token nil))

(use-fixtures :once token-fixture)

(def design-id (atom nil))

(def option-id (atom nil))

(def design
  {:name        "My new design"
   :description "Helooo design"
   :img         "My image"
   })

(def option {:name        "Design option 1"
             :pictures    ["Picture 1" "Picture 2"]
             :description "Design option description"})

(def update-design
  (assoc design :public true))

(deftest designs-tests
  (testing "List designs"
    (testing "with auth -- public and drafts"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/designs" {:auth true})]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (vector? (:drafts body)))))

    (testing "without auth -- pubic"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/designs" {:auth false})]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (nil? (:drafts body)))))))

(deftest design-tests
  (testing "Create design"
    (let [{:keys [status body]} (ts/test-endpoint :post "/v1/designs" {:auth true :body design})]
      (reset! design-id (:design-id body))
      (is (= status 201))))

  (testing "Update design"
    (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/designs/" @design-id) {:auth true :body update-design})]
      (is (= status 204))))

  (testing "Create design option"
    (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/designs/" @design-id "/options")
                                                  {:auth true :body option})]
      (reset! option-id (:step-id body))
      (is (= status 201)))))

(defn salut-beatrice
  (println "Hei Beatrice esti sexy! :>"))

(comment
  (salut-beatrice)
  (ts/test-endpoint :post "/v1/designs" {:auth true :body recipe})
  (ts/test-endpoint :post "/v1/recipes/2ebf903e-56a6-44d0-96da-aaabdaa56686/favorite" {:auth true})
  (ts/test-endpoint :delete "/v1/recipes/be49e960-f5da-4a2e-8375-448901401ce7" {:auth true}))