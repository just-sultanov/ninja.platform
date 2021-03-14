(ns ninja.javac
  (:refer-clojure :exclude [compile])
  (:gen-class)
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as maven])
  (:import
    (java.io
      IOException)
    (java.nio.file
      FileSystemLoopException
      FileVisitOption
      FileVisitResult
      FileVisitor
      Files
      LinkOption
      NoSuchFileException
      Path
      Paths)
    (java.nio.file.attribute
      BasicFileAttributes
      FileAttribute)
    (java.util
      EnumSet)))


(set! *warn-on-reflection* true)


(defn ^Path make-path
  "Converts a path string, or a sequence of strings that when joined form a path string, to a Path.
  Returns an instance of `java.nio.file.Path`."
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
  "Accepts handlers and returns an instance of `java.nio.file.FileVisitor`."
  [{:keys [visit-file-fn visit-file-failed-fn pre-visit-directory-fn post-visit-directory-fn]
    :or   {pre-visit-directory-fn  (constantly FileVisitResult/CONTINUE)
           post-visit-directory-fn (constantly FileVisitResult/CONTINUE)
           visit-file-fn           (constantly FileVisitResult/CONTINUE)
           visit-file-failed-fn    (constantly FileVisitResult/SKIP_SUBTREE)}}]
  (reify FileVisitor
    (preVisitDirectory [_ dir attrs] (pre-visit-directory-fn dir attrs))
    (visitFile [_ file attrs] (visit-file-fn file attrs))
    (visitFileFailed [_ file e] (visit-file-failed-fn file e))
    (postVisitDirectory [_ dir e] (post-visit-directory-fn dir e))))


(defn as-path
  "Ensures an instance of `java.nio.file.Path`."
  [x]
  (if (instance? Path x) x (make-path x)))


(defn find-paths
  "Returns a collection of paths in the given directory filtered using the provided predicate."
  ([root]
    (find-paths root (constantly true)))

  ([root pred]
    (let [*paths (atom [])]
      (Files/walkFileTree (as-path root) (EnumSet/of FileVisitOption/FOLLOW_LINKS) Integer/MAX_VALUE
        (file-visitor
          {:visit-file-fn        (fn [^Path file ^BasicFileAttributes attrs]
                                   (when (pred file attrs)
                                     (swap! *paths conj file))
                                   FileVisitResult/CONTINUE)
           :visit-file-failed-fn (fn [_ ^IOException e]
                                   (cond
                                     (instance? FileSystemLoopException e) FileVisitResult/SKIP_SUBTREE
                                     (instance? NoSuchFileException e) FileVisitResult/SKIP_SUBTREE
                                     :else (throw e)))}))
      @*paths)))


(defn find-java-paths
  "Returns a collection of paths to the Java source files in the given directory."
  [path]
  (find-paths path
    (fn [^Path path ^BasicFileAttributes attrs]
      (and (.isRegularFile attrs)
        (string/ends-with? path ".java")))))


(defn exists?
  "Returns `true` if the file exists. Otherwise, `false`."
  ([path]
    (exists? path [LinkOption/NOFOLLOW_LINKS]))

  ([path link-opts]
    (Files/exists (as-path path) (into-array LinkOption link-opts))))


(defn create-dirs!
  "Creates directories using `java.nio.Files/createDirectories`. Also creates parents if needed."
  ([root]
    (create-dirs! root []))

  ([root attrs]
    (Files/createDirectories (as-path root) (into-array FileAttribute attrs))))


(defn delete-dirs!
  "Deletes the file tree. Doesn't follow symbolic links."
  [root]
  (Files/walkFileTree (as-path root) #{} Integer/MAX_VALUE
    (file-visitor
      {:visit-file-fn           (fn [^Path file _] (Files/delete file) FileVisitResult/CONTINUE)
       :post-visit-directory-fn (fn [^Path dir _] (Files/delete dir) FileVisitResult/CONTINUE)
       :visit-file-failed-fn    (fn [_ ^IOException e]
                                  (cond
                                    (instance? FileSystemLoopException e) FileVisitResult/SKIP_SUBTREE
                                    (instance? NoSuchFileException e) FileVisitResult/SKIP_SUBTREE
                                    :else (throw e)))})))
