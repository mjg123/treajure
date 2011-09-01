(ns cljprj.core
  (:use [clojure.contrib.string :only [trim]]
        [cljprj.either])
  (:require [cljprj.persistence :as db]))

(def required-fields [:name :group-id :artifact-id])
(def all-fields (concat required-fields [:author :version :homepage :source-url :readme-text :tags]))

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
           (trim (.replaceAll (second %2) "\"" "'"))  ;(
           (second %2)))
      {}
      only-valid-fields)))

(defn make-location-url
  "Makes the URL for a single project"
  [prj]
  (str "/api/projects/" (prj :group-id) "/" (prj :artifact-id)))

(defn add-project [prj]
  (if (valid-project? prj)

    (let [prj (clean-project prj)
          [error result] (db/add-project (clean-project prj))]
      (if result
        {:status 201
         :headers {"location" (make-location-url prj)}
         :body {:message "yum, thanks"}}
        (make-error 409 error)))

    (make-error 400 "uploaded project must have at least #{ :name :group-id :artifact-id }")))

(defn get-project
  "retrieves a single project"
  [gid aid]
  (let [result (db/get-project gid aid)]
    (if result
      {:body result}
      no-such-project)))

(defn rm-project
  "removes a project from the service"
  [gid aid]
  (let [result (db/rm-project gid aid)]
    (if result
      {:status 204 :body nil}
      no-such-project)))

(defn attach-href
  "Adds the :href attribute to a project"
  [prj]
  (assoc prj :href (make-location-url prj)))

(defn list-projects [{name "name" tag "tag"}]

  (if (not-any? true? [(nil? name) (string? name)])
    (make-error 400 "name must be specified 0 or 1 times")

    (let [results (db/list-projects name tag)]
      {:body {:results (apply vector (map attach-href (reverse results)))}})))