(ns cljprj.either)

;; pretty straightforward.
;; see http://blog.robert-campbell.com/borrowing-haskells-either-for-clojure-error-h
(defn left  [v] [v nil])
(defn right [v] [nil v])