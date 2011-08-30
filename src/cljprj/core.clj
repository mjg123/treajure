(ns cljprj.core
  (:use [clojure.contrib.string :only [trim]])
  (:require [cljprj.persistence :as db]))

(def required-fields [:name :group-id :artifact-id])
(def all-fields (concat required-fields [:author :version :source-url :readme-text :tags]))

(defn make-error [code message]
  {:status code :body {:error message}})

(def no-such-project (make-error 404 "No such project"))

(defn valid-project?
  "Checks if the project contains the minimal amount of data to be valid"
  [prj-data]
  (and
    (map? prj-data)
    (every? #(contains? prj-data %) required-fields)
    (every? #(not= "" (trim (prj-data %))) required-fields)))

(defn clean-project
  "Removes unwanted fields from the project"
  [prj]
  (let [only-valid-fields (select-keys prj all-fields)]
    (reduce
      #(assoc %1 (first %2)
         (if (string? (second %2))
           (trim (.replaceAll (second %2) "\"" "'"))
           (second %2)))
      {}
      only-valid-fields)))

(defn make-location-url
  "Makes the URL for a single project"
  [prj]
  (str "/api/projects/" (prj :group-id) "/" (prj :artifact-id)))

(defn- coords
  "Create the value to be used in the mongo index.
As get is always by gid/aid we just need those two things."
  [gid aid]
  (str gid " $$$ " aid))

(defn- prj-coords [prj]
  (coords (prj :group-id) (prj :artifact-id)))

(defn add-project [prj]
  (if (valid-project? prj)
    (let [prj (clean-project prj)]
      (db/add-project {:project prj :coords-idx (prj-coords prj)})
      {:status 201
       :headers {"location" (make-location-url prj)}
       :body {:message "yum, thanks"}})
    (make-error 400 "uploaded project must have at least #{ :name :group-id :artifact-id }")))

(defn get-project
  "retrieves a single project"
  [gid aid]
  (let [result (db/get-project (coords gid aid))]
    (if result
      {:body result}
      no-such-project)))

(defn rm-project
  "removes a project from the service"
  [gid aid]
  (let [result (db/rm-project (coords gid aid))]
    (if result
      {:status 204 :body nil}
      no-such-project)))

(defn attach-href
  "Adds the :href attribute to a project"
  [prj]
  (assoc prj :href (make-location-url prj)))

(defn- name-filter [name results]
  (if (nil? name) results
    (filter #(.contains (% :name) name) results)))

(defn list-projects [{name "name" tags "tags"}]

  (if (not-any? true? [(nil? name) (string? name)])
    (make-error 400 "name must be specified 0 or 1 times")

    (let [raw-results (db/list-projects)
          filtered-results (name-filter name raw-results)]
      {:body {:results (apply vector (map attach-href (reverse filtered-results)))}})))