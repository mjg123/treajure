(ns treajure.test.treajure-driver
  (:require [treajure.test.rest-driver.rest-driver :as drv]
            [clojure.data.json :as json]))

(def ^{:private true
       :doc "Store which projects have been created to we can remove them later"}
  used-projects (atom #{}))

(def token (System/getenv "TREAJURE_TOKEN"))

(defn clear-project
  "Remove a project"
  [prj]
  (drv/DELETE (str "/api/projects/" (prj :group-id) "/" (prj :artifact-id) "?token=" token)))

(defn clear-project-without-token
  "Remove a project"
  [prj]
  (drv/DELETE (str "/api/projects/" (prj :group-id) "/" (prj :artifact-id))))

(defn make-project
  "Creates a new project for upload"
  [id]
  (let [new-project {:name (str "Test project " id)
                     :group-id (str "treajure-test-group-id-" id)
                     :artifact-id (str "treajure-test-artifact-id-" id)}]
    (clear-project new-project)
    (swap! used-projects conj new-project)
    new-project))

(defn make-unique-project
  "creates a minimal, unique project"
  []
  (make-project (str (java.util.UUID/randomUUID))))

(defn make-full-project
  "Creates a new project for upload with valid entries for all fields"
  [id]
  (let [new-project (make-project id)]
    (assoc new-project
      :author "Matthew"
      :version "1.0.0"
      :source-url "http://mjg123.github.com"
      :readme-text "This is some text for a readme"
      :tags ["tag1" "tag2" "tag3"])))


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

(defn add-project-clj-utf8
  "Upload a project as application/clojure"
  [project]
  (drv/PUT "/api/projects"
    (drv/body (pr-str project) "application/clojure; charset=UTF-8")))

(defn add-project-json [project]
  "Upload a project as application/json"
  (drv/PUT "/api/projects"
    (drv/body (json/json-str project) "application/json")))


(def accept-clj (drv/header "accept" "application/clojure"))
(def accept-json (drv/header "accept" "application/json"))
