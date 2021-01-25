(ns designvote.design-tests
  (:require [clojure.test :refer :all]
            [designvote.server :refer :all]
            [designvote.test-system :as ts]
            [clojure.spec.alpha :as s]))

(defn token-fixture
  "Fixture to create a new user with auth0, retrieve correct token for tests and delete user at cleanup"
  [f]
  (ts/create-auth0-user {:connection "Username-Password-Authentication"
                         :email      "account-testing@designvote.io"
                         :password   "Sepulcral94"})
  (reset! ts/token (ts/get-test-token "account-testing@designvote.io"))
  (ts/test-endpoint :post "/v1/account" {:auth true})
  (f)
  (ts/test-endpoint :delete "/v1/account" {:auth true})
  (reset! ts/token nil))

(use-fixtures :once token-fixture)

(def design-id (atom nil))

(def version-id (atom nil))

(def design
  {:name        "My new design"
   :description "Helooo design"
   :img         "My image"
   })

(def version {:name        "Design option 1"
              :pictures    ["Picture 1" "Picture 2"]
              :description "Design option description"})

(def new-version (assoc version :name "Updated design version"))

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

  (testing "Add version to design"
    (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/designs/" @design-id "/versions")
                                                  {:auth true :body version})]
      (reset! version-id (:version-id body))
      (is (= status 201))
      (is (string? (:version-id body)))))


  (testing "Update design version"
    (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/designs/" @design-id "/versions")
                                                  {:auth true :body (assoc new-version
                                                                      :version-id @version-id)})]
      (is (= status 204))))


  (testing "Delete design"
    (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/designs/" @design-id) {:auth true})]
      (is (= status 204)))))



(comment
  (println update-design)
  (ts/test-endpoint :post "/v1/designs" {:auth true :body design})
  (ts/test-endpoint :post (str "/v1/designs/f012da67-4172-4205-8011-31a69527107d/options")
                    {:auth true :body option})
  (ts/test-endpoint :put (str "/v1/designs/f012da67-4172-4205-8011-31a69527107d/options")
                    {:auth true :body (assoc)})
  (ts/test-endpoint :get "/v1/designs/f012da67-4172-4205-8011-31a69527107d" {:auth true})
  (ts/test-endpoint :put "/v1/designs/c5a1674e-fb49-493f-b221-f5e44609174d" {:auth true
                                                                             :body update-design})
  (ts/test-endpoint :delete "/v1/recipes/be49e960-f5da-4a2e-8375-448901401ce7" {:auth true}))