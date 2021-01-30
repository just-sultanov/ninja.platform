(ns ninja.response-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ninja.response :as sut]))


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
