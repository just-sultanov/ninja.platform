(ns ninja.javac-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ninja.javac :as sut]))


(deftest ^:unit square-test
  (testing "dummy test"
    (is (= 4 (sut/square 2)))))
