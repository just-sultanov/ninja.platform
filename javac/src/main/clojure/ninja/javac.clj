(ns ninja.javac
  (:refer-clojure :exclude [compile])
  (:gen-class)
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as maven])
  (:import
    (java.nio.file
      FileSystemLoopException
      FileVisitOption
      FileVisitResult
      FileVisitor
      Files
      NoSuchFileException
      Path
      Paths)
    (java.nio.file.attribute
      BasicFileAttributes)
    (java.util
      EnumSet)))


(set! *warn-on-reflection* true)


(defn ^Path make-path
  "Converts a path string, or a sequence of strings that when joined form a path string, to a Path.
  Returns an instance of `java.nio.file.Path`"
  [path & paths]
  (->> paths
    (map str)
    (into-array String)
    (Paths/get (str path))))


(defn make-classpath
  "Returns a calculated classpath taken from deps."
  ([]
    (make-classpath {}))

  ([{:keys [deps-map aliases]}]
    (let [deps-map (or deps-map (deps/slurp-deps (io/file "deps.edn")))
          deps-map (update deps-map :mvn/repos #(merge maven/standard-repos %))
          paths    (select-keys deps-map [:paths])
          args-map (deps/combine-aliases deps-map aliases)]
      (as-> deps-map $
        (deps/resolve-deps $ args-map)
        (deps/make-classpath-map paths $ args-map)
        (get $ :classpath-roots)
        (deps/join-classpath $)))))


(defn file-visitor
  "Accepts `visitor-fn` and returns an instance of `java.nio.file.FileVisitor`."
  [visitor-fn]
  (reify FileVisitor
    (postVisitDirectory [_ _ _] FileVisitResult/CONTINUE)
    (preVisitDirectory [_ _ _] FileVisitResult/CONTINUE)
    (visitFile [_ path attrs] (visitor-fn path attrs))
    (visitFileFailed [_ _ e]
      (cond
        (instance? FileSystemLoopException e) FileVisitResult/SKIP_SUBTREE
        (instance? NoSuchFileException e) FileVisitResult/SKIP_SUBTREE
        :else (throw e)))))


(defn as-path
  "Ensures an instance of `java.nio.file.Path`."
  [x]
  (if (instance? Path x) x (make-path x)))


(defn find-paths
  "Returns a collection of paths in the given directory filtered using the provided predicate."
  ([in]
    (find-paths in (constantly true)))

  ([in pred]
    (let [*paths  (atom [])
          visitor (file-visitor
                    (fn [^Path path ^BasicFileAttributes attrs]
                      (when (pred path attrs)
                        (swap! *paths conj path))
                      FileVisitResult/CONTINUE))]
      (Files/walkFileTree (as-path in) (EnumSet/of FileVisitOption/FOLLOW_LINKS) Integer/MAX_VALUE visitor)
      @*paths)))


(defn find-java-paths
  "Returns a collection of paths to the Java source files in the given directory."
  [path]
  (find-paths path
    (fn [^Path path ^BasicFileAttributes attrs]
      (and (.isRegularFile attrs)
        (string/ends-with? path ".java")))))
