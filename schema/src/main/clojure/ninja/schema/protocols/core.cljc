(ns ninja.schema.protocols.core
  #?(:cljs
     (:refer-clojure :exclude [-name])))


(defprotocol IntoSchema
  (-into-schema [builder x] [builder x opts]))


(defprotocol Schema
  (-name [schema])
  (-version [schema])
  (-doc [schema])
  (-form [schema])
  (-validator [schema] [schema opts])
  (-explainer [schema] [schema opts])
  (-generator [schema] [schema opts]))
