(ns ninja.schema.protocols.generator)


(defprotocol IntoGenerator
  (-into-generator [builder] [builder opts]))


(defprotocol Generator
  (-generate [generator] [generator opts]))
