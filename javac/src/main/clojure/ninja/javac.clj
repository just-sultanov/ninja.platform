(ns ninja.javac
  (:refer-clojure :exclude [compile])
  (:gen-class)
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as maven]
    [io.pedestal.log :as log]
    [ninja.response :as r])
  (:import
    (java.io
      ByteArrayOutputStream
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
      EnumSet)
    (javax.tools
      JavaCompiler
      ToolProvider)))


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
  [root]
  (find-paths root
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


(defn make-command
  "Builds and returns a Java compiler command."
  [{:keys [classpath target-path compiler-options source-paths]
    :or   {target-path      "classes"
           compiler-options []
           source-paths     []}}]
  `["-cp" ~classpath
    ~@compiler-options
    "-d" ~(str target-path)
    ~@(map str source-paths)])


;; TODO: [2021-03-14, just.sultanov@gmail.com] Set `:verbose?` option to false (by default)
(defn compile
  "A simple wrapper for the Java source compiler.

  Params:
  * `:source-path`      - Path to java sources (Default: \"src/main/java\")
  * `:target-path`      - Files are compiled to the target path (Default: \"classes\")
  * `:compiler-options` - Java compiler options (Default: [])
  * `:deps-map`         - Deps configuration map. Reads `deps.edn` by default.
  * `:aliases`          - Additional aliases from `deps.edn` (Default: [])
  * `:verbose?`         - Print extra info (Default: true)
  * `:compile?`         - Is compilation enabled?. Can be useful for debugging. (Default: true)"
  [{:as   options
    :keys [source-path target-path compiler-options deps-map aliases verbose? compile?]
    :or   {source-path      "src/main/java"
           target-path      "classes"
           compiler-options []
           aliases          []
           verbose?         true
           compile?         true}}]
  (log/info :ninja.javac.compile/starting options)
  (let [compiler ^JavaCompiler (ToolProvider/getSystemJavaCompiler)]
    (if-not compiler
      (let [result {:message "Can't find the Java compiler"
                    :options options}]
        (log/error :ninja.javac.compile/failed result)
        (r/as-error result))
      (let [source-path  (make-path source-path)
            target-path  (make-path target-path)
            source-paths (find-java-paths source-path)]
        (if-not (seq source-paths)
          (let [result {:message "Can't find the Java source files"
                        :options options}]
            (log/error :ninja.javac.compile/failed result)
            (r/as-error result))
          (let [classpath    (make-classpath {:deps-map deps-map, :aliases aliases})
                compile-opts {:classpath        classpath
                              :target-path      target-path
                              :compiler-options compiler-options
                              :source-paths     source-paths}
                command      (make-command compile-opts)
                result       (cond-> {:message (format "Processed %s files" (count source-paths))
                                      :options options}
                               verbose? (assoc :command command :compile-opts compile-opts))]
            (if-not compile?
              (do
                (log/info :ninja.javac.compile/completed result)
                (r/as-success result))
              (let [_      (create-dirs! target-path)
                    out    (ByteArrayOutputStream.)
                    err    (ByteArrayOutputStream.)
                    code   (.run compiler nil out err (into-array String command))
                    result (assoc result :compilation-result {:code code, :info (str out), :warnings (str err)})]
                (log/info :ninja.javac.compile/completed result)
                (if (zero? code)
                  (r/as-success result)
                  (r/as-error (assoc result :message "Something went wrong")))))))))))


;; TODO: [2021-03-14, just.sultanov@gmail.com] Add CLI: provide all options and add help
(defn -main
  "The entry point to run compiler using `clojure -M`. Use the `compile` function directly."
  [& [source-path target-path deps-map aliases verbose? compile? & compiler-options]]
  (let [res (compile
              {:source-path      source-path
               :target-path      target-path
               :deps-map         (edn/read-string deps-map)
               :compiler-options compiler-options
               :aliases          (edn/read-string aliases)
               :verbose?         (edn/read-string verbose?)
               :compile?         (edn/read-string compile?)})]
    (when (r/error? res)
      (System/exit 1))))


(comment
  ;; example
  (compile {:source-path      "src/test/resources/fixtures"
            :target-path      "target/classes"
            :compiler-options ["-target" "15"
                               "-source" "15"
                               "-Xlint:all"]
            :aliases          [:module.test/deps] ;; adds `ninja.platform/schema:0.0.1-alpha1` for testing classpath calculation
            :verbose?         false
            :compile?         true})

  ;; a similar command for the main entry point
  (-main
    "src/test/resources/fixtures" ;; :source-path
    "target/classes" ;; :target-path
    "nil" ;; :deps-map
    "[:module.test/deps]" ;; :aliases
    "true" ;; :verbose?
    "true" ;; :compile?
    "-target" "15" "-source" "15" "-Xlint:all" ;; :compiler-options
    )
  )
