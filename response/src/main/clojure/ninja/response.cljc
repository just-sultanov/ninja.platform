(ns ninja.response
  "A Clojure(Script) library for the unified responses."
  (:refer-clojure :exclude [type meta #?(:cljs -meta)])
  #?(:clj
     (:import
       (clojure.lang
         PersistentArrayMap
         PersistentHashMap)
       (java.io
         Writer)))
  #?(:cljs
     (:require
       [cljs.reader :as reader])))


#?(:clj
   (set! *warn-on-reflection* true))


(defonce
  ^{:doc   "Registry of anomalies."
    :added "0.0.1"}
  *anomalies
  (atom
    #{:error
      :warning
      :exception
      :unavailable
      :interrupted
      :incorrect
      :unauthorized
      :forbidden
      :not-found
      :unsupported
      :conflict
      :busy
      :unknown}))


(defn add-anomaly!
  "Adds a new anomaly to the anomalies registry."
  {:added "0.0.1"}
  [anomaly]
  (swap! *anomalies conj anomaly)
  anomaly)


(defn remove-anomaly!
  "Removes an anomaly from the anomalies registry."
  {:added "0.0.1"}
  [anomaly]
  (swap! *anomalies disj anomaly)
  anomaly)


;; TODO: [2021-01-29, just.sultanov] support relations?
;; (or (boolean (@*anomalies x)) (isa? x ::error))

(defn anomaly?
  "Returns `true` if `x` is an anomaly. Otherwise, `false`."
  {:added "0.0.1"}
  [x]
  (boolean (@*anomalies x)))



;;;;
;; Unified response protocol
;;;;

(defprotocol IResponse
  "A unified response protocol."
  :extend-via-metadata true
  (-error? [this] "Returns `true` if `type` of a unified response is an anomaly. Otherwise, `false`.")
  (-type [this] "Returns `type` of a unified response.")
  (-data [this] "Returns `data` of a unified response.")
  (-meta [this] "Returns `meta` of a unified response."))


(defn error?
  "Returns `true` if is a unified error response. Otherwise, `false`."
  {:added "0.0.1"}
  [x]
  (-error? x))


(defn type
  "Returns `type` of a unified response."
  {:added "0.0.1"}
  [x]
  (-type x))


(defn data
  "Returns `data` of a unified response."
  {:added "0.0.1"}
  [x]
  (-data x))


(defn meta
  "Returns `meta` of a unified response."
  {:added "0.0.1"}
  [x]
  (-meta x))



;;;;
;; Unified response
;;;;

(def tag
  #?(:clj  (.intern "#ninja/response")
     :cljs "#ninja/response"))


(defrecord Response
  [type data meta]
  IResponse
  (-error? [_] (anomaly? type))
  (-type [_] type)
  (-data [_] data)
  (-meta [_] meta)

  Object
  (toString [this] (str tag (into {} this))))


#?(:clj
   (defmethod print-method Response [response ^Writer writer]
     (.write writer ^String tag)
     (print-method (into {} response) writer)))


#?(:clj
   (defmethod print-dup Response [response ^Writer writer]
     (print-dup (into {} response) writer)))


#?(:cljs
   (extend-type Response
     IPrintWithWriter
     (-pr-writer [this writer _opts]
       (-write writer this))))


#?(:cljs
   (reader/register-tag-parser! 'ninja/response map->Response))



;;;;
;; Unified response builder
;;;;

(defn as-response
  "Returns an instance of a unified response."
  {:added "0.0.1"}
  ([type] (as-response type nil nil))
  ([type data] (as-response type data nil))
  ([type data meta] (->Response type data meta)))



;;;;
;; Unified error response helpers
;;;;

(defn as-error
  "Returns a unified `error` response."
  {:added "0.0.1"}
  ([data] (as-error data nil))
  ([data meta] (as-response :error data meta)))


(defn as-warning
  "Returns a unified `warning` response."
  {:added "0.0.1"}
  ([data] (as-warning data nil))
  ([data meta] (as-response :warning data meta)))


(defn as-exception
  "Returns a unified `exception` response."
  {:added "0.0.1"}
  ([data] (as-exception data nil))
  ([data meta] (as-response :exception data meta)))


(defn as-unavailable
  "Returns a unified `unavailable` response."
  {:added "0.0.1"}
  ([data] (as-unavailable data nil))
  ([data meta] (as-response :unavailable data meta)))


(defn as-interrupted
  "Returns a unified `interrupted` response."
  {:added "0.0.1"}
  ([data] (as-interrupted data nil))
  ([data meta] (as-response :interrupted data meta)))


(defn as-incorrect
  "Returns a unified `incorrect` response."
  {:added "0.0.1"}
  ([data] (as-incorrect data nil))
  ([data meta] (as-response :incorrect data meta)))


(defn as-unauthorized
  "Returns a unified `unauthorized` response."
  {:added "0.0.1"}
  ([data] (as-unauthorized data nil))
  ([data meta] (as-response :unauthorized data meta)))


(defn as-forbidden
  "Returns a unified `forbidden` response."
  {:added "0.0.1"}
  ([data] (as-forbidden data nil))
  ([data meta] (as-response :forbidden data meta)))


(defn as-not-found
  "Returns a unified `not-found` response."
  {:added "0.0.1"}
  ([data] (as-not-found data nil))
  ([data meta] (as-response :not-found data meta)))


(defn as-unsupported
  "Returns a unified `unsupported` response."
  {:added "0.0.1"}
  ([data] (as-unsupported data nil))
  ([data meta] (as-response :unsupported data meta)))


(defn as-conflict
  "Returns a unified `conflict` response."
  {:added "0.0.1"}
  ([data] (as-conflict data nil))
  ([data meta] (as-response :conflict data meta)))


(defn as-busy
  "Returns a unified `busy` response."
  {:added "0.0.1"}
  ([data] (as-busy data nil))
  ([data meta] (as-response :busy data meta)))


(defn as-unknown
  "Returns a unified `unknown` response."
  {:added "0.0.1"}
  ([data] (as-unknown data nil))
  ([data meta] (as-response :unknown data meta)))



;;;;
;; Unified success response helpers
;;;;

(defn as-success
  "Returns a unified `success` response."
  {:added "0.0.1"}
  ([data] (as-success data nil))
  ([data meta] (as-response :success data meta)))


(defn as-found
  "Returns a unified `found` response."
  {:added "0.0.1"}
  ([data] (as-found data nil))
  ([data meta] (as-response :found data meta)))


(defn as-created
  "Returns a unified `created` response."
  {:added "0.0.1"}
  ([data] (as-created data nil))
  ([data meta] (as-response :created data meta)))


(defn as-updated
  "Returns a unified `updated` response."
  {:added "0.0.1"}
  ([data] (as-updated data nil))
  ([data meta] (as-response :updated data meta)))


(defn as-deleted
  "Returns a unified `deleted` response."
  {:added "0.0.1"}
  ([data] (as-deleted data nil))
  ([data meta] (as-response :deleted data meta)))


(defn as-accepted
  "Returns a unified `accepted` response."
  {:added "0.0.1"}
  ([data] (as-accepted data nil))
  ([data meta] (as-response :accepted data meta)))



;;;;
;; Helper macros
;;;;

#?(:clj
   (defn cljs?
     "Checks &env in macro and returns `true` if that cljs env. Otherwise `false`."
     {:added "0.0.1"}
     [env]
     (boolean (:ns env))))


#?(:clj
   (defmacro safe
     "Extended version of try-catch.
     Usage:
      * (safe (/ 1 0))                                ;; => nil
      * (safe (/ 1 0) #(ex-message %))                ;; => \"Divide by zero\"
      * (safe (/ 1 0) #(as-exception (ex-message %))) ;; => => #ninja/response{:type :exception, :data \"Divide by zero\", :meta nil}"
     {:added "0.0.1"}
     ([body]
       `(try
          ~body
          (catch ~(if-not (cljs? &env) 'Exception :default) ~'_)))

     ([body with]
       `(try
          ~body
          (catch ~(if-not (cljs? &env) 'Exception :default) error#
            (~with error#))))))



;;;;
;; Extend protocol for compatibility
;;;;

(extend-protocol IResponse
  nil
  (-error? [_] false)
  (-type [_] nil)
  (-data [_] nil)
  (-meta [_] nil))


#?(:clj
   (extend-protocol IResponse
     Object
     (-error? [_] false)
     (-type [_] nil)
     (-data [this] this)
     (-meta [_] nil)))


#?(:cljs
   (extend-protocol IResponse
     default
     (-error? [_] false)
     (-type [_] nil)
     (-data [this] this)
     (-meta [_] nil)))


#?(:clj
   (extend-protocol IResponse
     PersistentArrayMap
     (-error? [this] (anomaly? (-type this)))
     (-type [this] (get this :type))
     (-data [this] (get this :data))
     (-meta [this] (get this :meta))

     PersistentHashMap
     (-error? [this] (anomaly? (-type this)))
     (-type [this] (get this :type))
     (-data [this] (get this :data))
     (-meta [this] (get this :meta))))


#?(:cljs
   (extend-protocol IResponse
     cljs.core/PersistentArrayMap
     (-error? [this] (anomaly? (-type this)))
     (-type [this] (get this :type))
     (-data [this] (get this :data))
     (-meta [this] (get this :meta))

     cljs.core/PersistentHashMap
     (-error? [this] (anomaly? (-type this)))
     (-type [this] (get this :type))
     (-data [this] (get this :data))
     (-meta [this] (get this :meta))))
