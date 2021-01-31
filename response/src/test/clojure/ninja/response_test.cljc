(ns ninja.response-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ninja.response :as sut #?@(:cljs [:refer [Response]])])
  #?(:clj
     (:import
       (ninja.response
         Response))))


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


(deftest ^:unit response-test
  (testing "as-response"
    (testing "1-arity - [type]"
      (let [type :response
            res  (sut/as-response type)]
        (is (instance? Response res))
        (is (satisfies? sut/IResponse res))
        (is (false? (sut/error? res)))
        (sut/add-anomaly! type)
        (is (true? (sut/error? res)))
        (is (= type (sut/type res)))
        (is (nil? (sut/data res)))
        (is (nil? (sut/meta res)))
        (sut/remove-anomaly! type)
        (is (false? (sut/error? res)))
        (is (= "#ninja/response{:type :response, :data nil, :meta nil}" (str res) (pr-str res)))))

    (testing "2-arity - [type data]"
      (let [type :response
            data 42
            res  (sut/as-response type data)]
        (is (instance? Response res))
        (is (satisfies? sut/IResponse res))
        (is (false? (sut/error? res)))
        (sut/add-anomaly! type)
        (is (true? (sut/error? res)))
        (is (= type (sut/type res)))
        (is (= data (sut/data res)))
        (is (nil? (sut/meta res)))
        (sut/remove-anomaly! type)
        (is (false? (sut/error? res)))
        (is (= "#ninja/response{:type :response, :data 42, :meta nil}" (str res) (pr-str res)))))

    (testing "3-arity - [type data meta]"
      (let [type :response
            data 42
            meta {:some :meta}
            res  (sut/as-response type data meta)]
        (is (instance? Response res))
        (is (satisfies? sut/IResponse res))
        (is (false? (sut/error? res)))
        (sut/add-anomaly! type)
        (is (true? (sut/error? res)))
        (is (= type (sut/type res)))
        (is (= data (sut/data res)))
        (is (= meta (sut/meta res)))
        (sut/remove-anomaly! type)
        (is (false? (sut/error? res)))
        (is (= "#ninja/response{:type :response, :data 42, :meta {:some :meta}}" (str res) (pr-str res)))))))
