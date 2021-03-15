(ns ninja.javac-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [deftest testing is]]
    [ninja.javac :as sut]
    [ninja.response :as r])
  (:import
    (java.nio.file
      Path)))


(deftest make-path-test
  (testing "build path"
    (is (= "a" (str (sut/make-path "a"))))
    (is (= "a/b/c" (str (sut/make-path "a/b" "c")) (str (sut/make-path "a" "b" "c"))))))


(deftest make-classpath-test
  (testing "calculate classpath"
    (let [includes? #(string/includes? % "ninja/platform/schema/0.0.1-alpha1/schema-0.0.1-alpha1.jar")]
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


(deftest create-delete-dirs-test
  (testing "directories creation and deletion"
    (let [root-path   "a"
          nested-path (sut/make-path root-path "b/c/d")]
      (testing "directory shouldn't be exists"
        (is (false? (sut/exists? nested-path))))
      (testing "directory should be created"
        (sut/create-dirs! nested-path)
        (is (true? (sut/exists? nested-path))))
      (testing "nested directory should be deleted"
        (sut/delete-dirs! nested-path)
        (is (false? (sut/exists? nested-path))))
      (testing "root directory should be deleted"
        (sut/delete-dirs! root-path)
        (is (false? (sut/exists? root-path)))))))


(deftest make-command-test
  (testing "build compiler command"
    (testing "with default options"
      (is (= ["-cp" "some:classpath" "-d" "classes" "fake/Example.java"]
            (sut/make-command {:classpath    "some:classpath"
                               :source-paths ["fake/Example.java"]}))))
    (testing "with all options"
      (is (= ["-cp" "some:classpath" "-target" "15" "-source" "15" "-Xlint:all" "-d" "target/classes" "fake/Example.java"]
            (sut/make-command {:classpath        "some:classpath"
                               :target-path      "target/classes"
                               :compiler-options ["-target" "15"
                                                  "-source" "15"
                                                  "-Xlint:all"]
                               :source-paths     ["fake/Example.java"]}))))))


(deftest compile-test
  (let [source-path      "src/test/resources/fixtures"
        target-path      (str "target/classes/" (System/nanoTime))
        compiler-options ["-Xlint:all"]
        aliases          [:module.test/deps]
        verbose?         true
        compile?         true
        options          {:source-path      source-path
                          :target-path      target-path
                          :compiler-options compiler-options
                          :aliases          aliases
                          :verbose?         verbose?
                          :compile?         compile?}
        source-paths     (->> source-path
                           sut/find-java-paths
                           (map str)
                           set)
        includes?        #(string/includes? % "ninja/platform/schema/0.0.1-alpha1/schema-0.0.1-alpha1.jar")]

    (testing "expected failed compilation result"
      (let [bad-options (update options :source-path str "/bad-source-path")
            {:as res :keys [type data meta]} (sut/compile bad-options)]

        (testing "as a unified response"
          (is (r/error? res))
          (is (= :error type))
          (is (nil? meta)))

        (testing "with correct message"
          (is (= "Can't find the Java source files" (:message data))))

        (testing "with provided options without any changes"
          (is (= bad-options (:options data))))))


    (testing "expected successful compilation result"
      (let [{:as res :keys [type data meta]} (sut/compile options)
            compile-opts (:compile-opts data)
            command      (:command data)]

        (testing "as a unified response"
          (is (false? (r/error? res)))
          (is (= :success type))
          (is (nil? meta)))

        (testing "with correct message"
          (is (= "Processed 8 files" (:message data))))

        (testing "with provided options without any changes"
          (is (= options (:options data))))

        (testing "with zero compilation status"
          (is (= {:code 0, :info "", :warnings ""} (:compilation-result data))))

        (testing "with correct compile options"
          (is (= target-path (str (:target-path compile-opts))))
          (is (= compiler-options (:compiler-options compile-opts)))
          (is (every? #(contains? source-paths (str %)) (:source-paths compile-opts)))
          (is (true? (includes? (:classpath compile-opts)))))

        (testing "with correct command"
          (is (= 13 (count command)))
          (is (= "-cp" (first command)))
          (is (= (:classpath compile-opts) (second command)))
          (is (true? (includes? (second command))))
          (is (= compiler-options (take-while (partial not= "-d") (nnext command))))
          (is (= ["-d" target-path] (->> command nnext (drop-while (partial not= "-d")) (take 2))))
          (is (every? #(contains? source-paths %) (->> command nnext (drop-while (partial not= target-path)) next))))))

    ;; cleanup
    (sut/delete-dirs! target-path)))
