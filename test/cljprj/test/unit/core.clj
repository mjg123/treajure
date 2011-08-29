(ns cljprj.test.unit.core
  (:use [midje.sweet]
        [cljprj.core :only [valid-project?]]))

;;;;  NB there is nothing to actually unit test yet - this is just an example

(def min-prj {:name "name" :group-id "gid" :artifact-id "aid"})

(facts "Facts about what makes a valid project"

  (fact "nil is not valid"
    (valid-project? nil) => false)

  (fact "a string is not valid"
    (valid-project? "foo") => false)

  (fact "an empty map is not valid"
    (valid-project? {}) => false)

  (fact "a minimal project IS valid"
    (valid-project? min-prj) => true)

  (fact "the 3 core keys are each necessary"
    (valid-project? (dissoc min-prj :name)) => false
    (valid-project? (dissoc min-prj :group-id)) => false
    (valid-project? (dissoc min-prj :artifact-id)) => false)

  (fact "the 3 core keys must each be non-empty"
    (valid-project? (assoc min-prj :name "")) => false
    (valid-project? (assoc min-prj :group-id "")) => false
    (valid-project? (assoc min-prj :artifact-id "")) => false)


  )