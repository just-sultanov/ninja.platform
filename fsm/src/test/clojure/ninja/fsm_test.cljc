(ns ninja.fsm-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [ninja.fsm :as sut]))


(deftest ^:unit dummy-test
  (is (= 4 (sut/square 2))))
