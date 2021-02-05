(ns ninja.schema.protocols.explainer)


(defprotocol IntoExplainer
  (-into-explainer [builder] [builder opts]))


(defprotocol Explainer
  (-explain [explainer x] [explainer x opts]))
