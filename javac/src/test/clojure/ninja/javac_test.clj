(ns ninja.javac-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [deftest testing is]]
    [ninja.javac :as sut]))


(deftest make-path-test
  (testing "build path"
    (is (= "a" (str (sut/make-path "a"))))
    (is (= "a/b/c" (str (sut/make-path "a/b" "c")) (str (sut/make-path "a" "b" "c"))))))


(deftest make-classpath-test
  (testing "calculate classpath"
    (let [includes? #(string/includes? % "ninja/platform/response/0.1.0/response-0.1.0.jar")]
      (testing "without any arguments"
        (is (false? (includes? (sut/make-classpath))))
        (is (false? (includes? (sut/make-classpath {:deps-map nil}))))
        (is (false? (includes? (sut/make-classpath {:deps-map {}})))))

      (testing "with provided aliases"
        (is (false? (includes? (sut/make-classpath {:deps-map {}, :aliases [:module.test/deps]}))))
        (is (true? (includes? (sut/make-classpath {:deps-map nil, :aliases [:module.test/deps]}))))
        (is (true? (includes? (sut/make-classpath {:aliases [:module.test/deps]}))))))))
