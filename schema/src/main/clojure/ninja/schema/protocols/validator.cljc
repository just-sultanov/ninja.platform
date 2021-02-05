(ns ninja.schema.protocols.validator)


(defprotocol IntoValidator
  (-into-validator [builder] [builder opts]))


(defprotocol Validator
  (-validate [validator x] [validator x opts]))
