(ns cljprj.test.cljprj-driver
  (:require [cljprj.test.rest-driver.rest-driver :as drv]
            [clojure.contrib.json :as json]))

(def ^{:private true
       :doc "Store which projects have been created to we can remove them later"}
  used-projects (atom #{}))

(defn clear-project
  "Remove a project"
  [prj]
  (drv/DELETE (str "/api/projects/" (prj :group-id) "/" (prj :artifact-id))))

(defn make-project
  "Creates a new project for upload"
  [id]
  (let [new-project {:name (str "Test project " id)
                     :group-id (str "cljprj-test-group-id-" id)
                     :artifact-id (str "cljprj-test-artifact-id-" id)}]
    (clear-project new-project)
    (swap! used-projects conj new-project)
    new-project))

(defn clear-all-used-projects!
  "Calls clear-project for all projects we've uploaded"
  []
  (doall (map clear-project @used-projects))
  (reset! used-projects #{}))

(defn add-project-clj
  "Upload a project as application/clojure"
  [project]
  (drv/PUT "/api/projects"
    (drv/body (pr-str project) "application/clojure")))

(defn add-project-json [project]
  "Upload a project as application/json"
  (drv/PUT "/api/projects"
    (drv/body (json/json-str project) "application/json")))


(def accept-clj (drv/header "accept" "application/clojure"))
(def accept-json (drv/header "accept" "application/json"))
