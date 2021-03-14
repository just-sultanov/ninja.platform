(ns ninja.javac
  (:refer-clojure :exclude [compile])
  (:gen-class)
  (:require
    [clojure.java.io :as io]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as maven])
  (:import
    (java.nio.file
      Path
      Paths)))


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
