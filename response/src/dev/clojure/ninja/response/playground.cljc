(ns ninja.response.playground
  (:require
    #?@(:clj
        [[clojure.edn :as edn]
         [criterium.core :as cc]]
        :cljs
        [[cljs.reader :as reader]])
    [ninja.response :as r]))

;;;;
;; Perf tests
;;;;

;; #ninja/response

(def test1
  (r/as-not-found "some data" {:some :meta}))


(r/error? test1) ;; => true
(r/type test1) ;; => :not-found
(r/data test1) ;; => "some data"
(r/meta test1) ;; => {:some :meta}


#?(:clj
   (comment
     (cc/quick-bench (r/error? test1)) ;; => 11,749988 ns
     (cc/quick-bench (r/type test1)) ;; => 2,855091 ns
     (cc/quick-bench (r/data test1)) ;; => 2,893347 ns
     (cc/quick-bench (r/meta test1)) ;; => 2,620480 ns
     ))


;; map

(def test2
  {:type :unavailable
   :data "some data"
   :meta {:some :meta}})


(r/error? test2) ;; => true
(r/type test2) ;; => :unavailable
(r/data test2) ;; => "some data"
(r/meta test2) ;; => {:some :meta}


#?(:clj
   (comment
     (cc/quick-bench (r/error? test2)) ;; => 72,403432 ns
     (cc/quick-bench (r/type test2)) ;; => 49,530052 ns
     (cc/quick-bench (r/data test2)) ;; => 51,559561 ns
     (cc/quick-bench (r/meta test2)) ;; => 50,291221 ns
     ))


;; anomalies

#?(:clj
   (comment
     (r/anomaly? :yo!)
     (r/add-anomaly! :yo!)
     (cc/quick-bench (r/anomaly? :yo!)) ;; => 10,928285 ns
     (r/remove-anomaly! :yo!)
     (cc/quick-bench (r/anomaly? :yo!)) ;; => 9,853907 ns
     ))



;;;;
;; Serialization
;;;;

(def test3
  (r/as-unknown "some data" {:some :meta}))


#?(:clj
   (comment
     (prn test3) ;; => #ninja/response{:type :unknown, :data "some data", :meta {:some :meta}}
     (str test3) ;; => "#ninja/response{:type :unknown, :data \"some data\", :meta {:some :meta}}"
     (pr-str test3) ;; => "#ninja/response{:type :unknown, :data \"some data\", :meta {:some :meta}}"
     (read-string (str test3)) ;; => #ninja/response{:type :unknown, :data \"some data\", :meta {:some :meta}}
     (edn/read-string {:readers *data-readers*} (str test3)) ;; => #ninja/response{:type :unknown, :data \"some data\", :meta {:some :meta}}
     )

   :cljs
   (comment
     (prn test3) ;; => #ninja/response{:type :unknown, :data "some data", :meta {:some :meta}}
     (str test3) ;; => "#ninja/response{:type :unknown, :data \"some data\", :meta {:some :meta}}"
     (pr-str test3) ;; => "#ninja/response{:type :unknown, :data \"some data\", :meta {:some :meta}}"
     (reader/read-string (pr-str test3)) ;; => {:type :unknown, :data "some data", :meta {:some :meta}}
     ))



;;;;
;; Some perf tests for clojure.core
;;;;

;; set

(def s #{:a :b :c})
(def *s (atom #{:a :b :c}))


#?(:clj
   (comment
     (cc/quick-bench (s :c)) ;; => 5,847651 ns
     (cc/quick-bench (@*s :c)) ;; => 7,352768 ns
     (cc/quick-bench (contains? s :c)) ;; => 33,806436 ns
     (cc/quick-bench (contains? @*s :c)) ;; => 35,020073 ns
     (cc/quick-bench (s :d)) ;; => 0,301598 ns
     (cc/quick-bench (@*s :d)) ;; => 1,885095 ns
     (cc/quick-bench (contains? s :d)) ;; => 32,616228 ns
     (cc/quick-bench (contains? @*s :d)) ;; => 34,524609 ns
     ))


;; map

(def m {:a 1 :b 2 :c 3})


#?(:clj
   (comment
     (cc/quick-bench (:c m)) ;; => 8,116170 ns
     (cc/quick-bench (get m :c)) ;; => 7,262926 ns
     (cc/quick-bench (:d m)) ;; => 4,308656 ns
     (cc/quick-bench (get m :d)) ;; => 3,358132 ns
     ))
