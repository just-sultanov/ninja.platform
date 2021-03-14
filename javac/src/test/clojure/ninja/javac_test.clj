(ns ninja.javac-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [deftest testing is]]
    [ninja.javac :as sut])
  (:import
    (java.nio.file
      Path)))


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


(deftest as-path-test
  (testing "expected an instance of `java.nio.file.Path`"
    (is (true? (instance? Path (sut/as-path "a/b/c"))))
    (is (true? (instance? Path (sut/as-path (sut/make-path "a/b/c")))))))


(deftest find-paths-test
  (testing "find paths"
    (testing "in the non-existing directory"
      (is (zero? (count (sut/find-paths "fake/path")))))
    (testing "in the fixtures directory"
      (is (= 9 (count (sut/find-paths "src/test/resources/fixtures")))))))


(deftest find-java-paths-test
  (testing "find paths to Java source files"
    (testing "in the non-existing directory"
      (is (zero? (count (sut/find-java-paths "fake/path")))))
    (testing "in the fixtures directory"
      (is (= 8 (count (sut/find-java-paths "src/test/resources/fixtures")))))))
