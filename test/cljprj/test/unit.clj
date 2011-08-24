(ns cljprj.test.unit
  (:use [midje.sweet]))

;;;;  NB there is nothing to actually unit test yet - this is just an example

(fact "Simple arithmatic works and stuff"
  (+ 1 1) => 2)