(ns ninja.response-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ninja.response :as sut]))


(deftest ^:unit anomalies-registry-test
  (testing "expected `false` when an anomaly is not registered"
    (is (false? (sut/anomaly? ::bad-request))))

  (testing "expected `true` when a new anomaly is registered"
    (sut/add-anomaly! ::bad-request)
    (is (true? (sut/anomaly? ::bad-request))))

  (testing "expected `false` after an anomaly is unregistered"
    (is (true? (sut/anomaly? ::bad-request)))
    (sut/remove-anomaly! ::bad-request)
    (is (false? (sut/anomaly? ::bad-request)))))


(deftest ^:unit response-helpers-test
  (let [data     42
        meta     {:some :meta}
        warning1 (sut/as-warning data)
        warning2 (sut/as-warning data meta)]
    (is (sut/error? warning1))
    (is (= data (sut/data warning1)))
    (is (nil? (sut/meta warning1)))

    (is (sut/error? warning2))
    (is (= data (sut/data warning2)))
    (is (= meta (sut/meta warning2)))))
