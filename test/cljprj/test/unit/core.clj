(ns cljprj.test.unit.core
  (:use [midje.sweet]
        [cljprj.core :only [valid-project? clean-project]]))

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

  (fact "the 3 core keys must each have more than just whitespace"
    (valid-project? (assoc min-prj :name " ")) => false
    (valid-project? (assoc min-prj :group-id "\t")) => false
    (valid-project? (assoc min-prj :artifact-id "  \t   ")) => false)
  )

(facts "Facts about cleaning projects"

  (fact "whitespace is trimmed"
    ((clean-project (assoc min-prj :name "   hello   ")) :name) => "hello")

  (fact "double-quotes are changed to single-quotes" ; to work around a bug in clojurescript's read-string
    ((clean-project (assoc min-prj :name "say \"ahh\"")) :name) => "say 'ahh'")

  (fact "unwanted properties are removed"
    (contains? (clean-project (assoc min-prj :HAHAHA "   hello   ")) :HAHAHA) => false)

  (fact "tags-as-vector is valid"
    ((clean-project (assoc min-prj :tags [:t1 :t2])) :tags) => [:t1 :t2])
  )