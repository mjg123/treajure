 (ns treajure.test.unit.core
  (:use [midje.sweet]
        [treajure.core :only [valid-project? clean-project missing-fields add-project get-project]])
  (:require [treajure.persistence :as db]))

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

  (fact "All properties except tags must be strings"
	(valid-project? (assoc min-prj :other-field {:map "map"})) => false
	(valid-project? (assoc min-prj :tags {:map "map"})) => false))

(facts "Facts about missing fields"
  (fact "when one field is present the remaining two are missing"
        (missing-fields {:name "name"}) => #{:group-id :artifact-id})

  (fact "when the project isn't a map all the fields are missing"
        (missing-fields '()) => #{:group-id :artifact-id :name})

  (fact "when all required fields are present there are no missing fields"
        (missing-fields min-prj) => #{}))

(facts "Facts about cleaning projects"

  (fact "whitespace is trimmed"
    ((clean-project (assoc min-prj :name "   hello   ")) :name) => "hello")

  (fact "double-quotes are changed to single-quotes" ; to work around a bug in clojurescript's read-string
    ((clean-project (assoc min-prj :name "say \"ahh\"")) :name) => "say 'ahh'")

  (fact "unwanted properties are removed"
    (contains? (clean-project (assoc min-prj :HAHAHA "   hello   ")) :HAHAHA) => false)

  (fact "tags-as-vector is valid"
        ((clean-project (assoc min-prj :tags [:t1 :t2])) :tags) => [:t1 :t2]))

(facts "Facts about adding projects"
       (def valid-response [() ()])
       (fact "valid project is created"
             (add-project min-prj) => (contains {:status 201})
             (provided
              (db/add-project min-prj) => valid-response))
       (fact "project that doesn't have required attributes should respond bad request"
             (add-project (dissoc min-prj :name)) => (contains {:status 400}))
       (fact "tags should be lower case"
             (add-project (assoc min-prj :tags ["TAG"])) => (contains {:status 201})
             (provided
              (db/add-project (checker [actual] (= (actual :tags) ["tag"]))) => valid-response)))

(facts "Facts about dependencies"
       (def resp {:group-id "group" :artifact-id "artifact" :version "1.2.3"})
       (def repeat-group-resp {:group-id "group" :artifact-id "group" :version "1.2.3"})
       (def no-version-resp {:group-id "group" :artifact-id "artifact"})

       (fact "group/artifact version"
             (:body (get-project "group" "artifact")) => (contains {:lein-dep "group/artifact 1.2.3"})
             (provided
              (db/get-project "group" "artifact") => resp))

       (fact "group/group version"
             (:body (get-project "group" "group")) => (contains {:lein-dep "group 1.2.3"})
             (provided
              (db/get-project "group" "group") => repeat-group-resp))

       (fact "group/artifact"
             (:body (get-project "group" "artifact")) => (contains {:lein-dep "group/artifact"})
             (provided
              (db/get-project "group" "artifact") => no-version-resp)))