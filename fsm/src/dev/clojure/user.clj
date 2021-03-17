(ns user
  "Development helper functions."
  (:require
    [cider.piggieback :as pb]
    [cljs.repl.node :as rn]
    [hashp.core]))


(defn cljs-repl
  []
  (pb/cljs-repl (rn/repl-env)))
