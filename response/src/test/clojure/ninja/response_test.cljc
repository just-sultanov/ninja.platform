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
        (is (= "#ninja/response{:type :response, :data nil, :meta nil}" (str res) (pr-str res)))

        (testing "should be returned an error response after changing the response type"
          (is (true? (sut/error? (assoc res :type :error))))
          (is (= 42 (sut/data (assoc res :data 42))))
          (is (= {:some {:meta :data}} (sut/meta (update res :meta assoc-in [:some :meta] :data)))))))

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
        (is (= "#ninja/response{:type :response, :data 42, :meta nil}" (str res) (pr-str res)))

        (testing "should be returned an error response after changing the response type"
          (is (true? (sut/error? (assoc res :type :error)))))))

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
        (is (= "#ninja/response{:type :response, :data 42, :meta {:some :meta}}" (str res) (pr-str res)))

        (testing "should be returned an error response after changing the response type"
          (is (true? (sut/error? (assoc res :type :error)))))))))


(deftest ^:unit response-helpers-test
  (let [error-helpers   {:error        sut/as-error
                         :warning      sut/as-warning
                         :exception    sut/as-exception
                         :unavailable  sut/as-unavailable
                         :interrupted  sut/as-interrupted
                         :incorrect    sut/as-incorrect
                         :unauthorized sut/as-unauthorized
                         :forbidden    sut/as-forbidden
                         :not-found    sut/as-not-found
                         :unsupported  sut/as-unsupported
                         :conflict     sut/as-conflict
                         :busy         sut/as-busy
                         :unknown      sut/as-unknown}
        success-helpers {:success  sut/as-success
                         :found    sut/as-found
                         :created  sut/as-created
                         :updated  sut/as-updated
                         :deleted  sut/as-deleted
                         :accepted sut/as-accepted}]
    (testing "error response helpers"
      (doseq [[type f] error-helpers]
        (testing type
          (testing "1-arity - [data]"
            (let [data 42
                  res  (f data)]
              (is (instance? Response res))
              (is (satisfies? sut/IResponse res))
              (is (true? (sut/error? res)))
              (is (= type (sut/type res)))
              (is (= data (sut/data res)))
              (is (nil? (sut/meta res)))))
          (testing "2-arity - [data meta]"
            (let [data 42
                  meta {:some :meta}
                  res  (f data meta)]
              (is (instance? Response res))
              (is (satisfies? sut/IResponse res))
              (is (true? (sut/error? res)))
              (is (= type (sut/type res)))
              (is (= data (sut/data res)))
              (is (= meta (sut/meta res))))))))

    (testing "success response helpers"
      (doseq [[type f] success-helpers]
        (testing type
          (testing "1-arity - [data]"
            (let [data 42
                  res  (f data)]
              (is (instance? Response res))
              (is (satisfies? sut/IResponse res))
              (is (false? (sut/error? res)))
              (is (= type (sut/type res)))
              (is (= data (sut/data res)))
              (is (nil? (sut/meta res)))))
          (testing "2-arity - [data meta]"
            (let [data 42
                  meta {:some :meta}
                  res  (f data meta)]
              (is (instance? Response res))
              (is (satisfies? sut/IResponse res))
              (is (false? (sut/error? res)))
              (is (= type (sut/type res)))
              (is (= data (sut/data res)))
              (is (= meta (sut/meta res))))))))))


(deftest ^:unit macros-helpers-test
  (testing "exceptions should be caught"
    (let [msg   "surprise!"
          boom! #(throw (ex-info msg {}))
          res1  (sut/safe (boom!))
          res2  (sut/safe (boom!) #(sut/as-exception (ex-message %)))]
      (is (false? (sut/error? res1)))
      (is (nil? (sut/type res1)))
      (is (nil? (sut/data res1)))
      (is (nil? (sut/meta res1)))
      (is (true? (sut/error? res2)))
      (is (= :exception (sut/type res2)))
      (is (= msg (sut/data res2)))
      (is (nil? (sut/meta res2))))))


(deftest ^:unit response-protocol-extension-test
  (testing "Object should be satisfied with IResponse protocol"
    (doseq [x [nil true false 1 #?(:clj 1/2) \c "string" :keyword ::keyword 'symbol 'user/symbol '() [] #{}]]
      (is (false? (sut/error? x)))
      (is (nil? (sut/type x)))
      (is (= x (sut/data x)))
      (is (nil? (sut/meta x)))))

  (testing "Hash-maps should be satisfied with IResponse protocol"
    (testing "should be returned an error response"
      (let [type :forbidden
            data "some data"
            meta {:some :meta}]
        (doseq [x [{:type type, :data data, :meta meta}
                   (hash-map :type type, :data data, :meta meta)]]
          (is (true? (sut/error? x)))
          (is (= type (sut/type x)))
          (is (= data (sut/data x)))
          (is (= meta (sut/meta x)))

          (testing "should be returned a success response after changing the response type"
            (is (false? (sut/error? (assoc x :type ::ok))))))))

    (testing "should be returned a success response"
      (let [type ::ok
            data "some data"
            meta {:some :meta}]
        (doseq [x [{:type type, :data data, :meta meta}
                   (hash-map :type type, :data data, :meta meta)]]
          (is (false? (sut/error? x)))
          (is (= type (sut/type x)))
          (is (= data (sut/data x)))
          (is (= meta (sut/meta x)))

          (testing "should be returned an error response after changing the response type"
            (is (true? (sut/error? (assoc x :type :forbidden))))))))))
