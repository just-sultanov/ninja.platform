(ns ninja.schema-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ninja.schema :as sut]))


(deftest ^:unit square-test
  (is (= 4 (sut/square 2))))
