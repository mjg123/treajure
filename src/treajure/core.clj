(ns treajure.core
  (:use [clojure.contrib.string :only [trim, lower-case, blank?]]
        [treajure.either]
        [clojure.set :only [difference]])
  (:require [treajure.persistence :as db]))

(def required-fields [:name :group-id :artifact-id])
(def all-fields (concat required-fields [:author :version :homepage :source-url :readme-text :tags]))

(defn make-error [code message]
  {:status code :body {:error message}})

(def no-such-project (make-error 404 "No such project"))

(defn valid-project-types?
  [prj-data]
  "Check if the project types are correct"
  (letfn [(valid? [[x y]] (if (= x :tags)
			(vector? y)
			(= String (class y))))]
    (every? valid? prj-data)))

(defn valid-project?
  "Checks if the project contains the minimal amount of data to be valid"
  [prj-data]
  (and
   (map? prj-data)
   (valid-project-types? prj-data)
   (every? #(contains? prj-data %) required-fields)
   (every? #(not= "" (trim (prj-data %))) required-fields)))

(defn fields-with-values
  "Returns the keys of the fields that don't have values"
  [prj-data]
  (map first (filter #(not= "" (second %)) prj-data)))
  
(defn missing-fields
  "Returns the fields that should be added to make the project valid"
  [prj-data]
  (if (map? prj-data)
    (difference (set required-fields) (set (fields-with-values prj-data)))
    (set required-fields)))

(defn clean-project
  "Removes unwanted fields from the project"
  [prj]
  (let [only-valid-fields (select-keys prj all-fields)]
    (reduce
      #(assoc %1 (first %2)
         (if (string? (second %2))
           (trim (.replaceAll (second %2) "\"" "'")) ;(
           (second %2)))
      {}
      only-valid-fields)))

(defn make-location-url
  "Makes the URL for a single project"
  [{group :group-id artifact :artifact-id}]
  (str "/api/projects/" group "/" artifact))

(defn lowercase-tags
  "Make the tags in the project lower case"
  [{tags :tags :as prj}]
  (if (seq tags)
    (assoc prj :tags (map lower-case tags))
    prj))

(defn add-project [prj]
  (if (valid-project? prj)
    (let [prj (-> (clean-project prj)
                  (lowercase-tags))
          [error result] (db/add-project prj)]
      (if result
        {:status 201
         :headers {"location" (make-location-url prj)}
         :body {:message "yum, thanks"}}
        (make-error 409 error)))

    (make-error 400 (str "uploaded project must have at least #{ :name :group-id :artifact-id }, you were missing" (missing-fields prj)))))

(defn create-deps [prj]
  "create the lein-deps property for the project"
  (let [grp (:group-id prj)
        art (:artifact-id prj)
        ver (:version prj)
        name (if (= grp art) grp (str grp "/" art))]
    (assoc prj :lein-dep (if (blank? ver) name (str name " " ver)))))

(defn get-project
  "retrieves a single project"
  [gid aid]
  (let [result (db/get-project gid aid)]
    (if result
      {:body (create-deps result)}
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

(defn lower-case-tag
  "Make tag lowercase"
  [tag]
  (cond
   (nil? tag) tag
   (vector? tag) (vec (map lower-case tag))
   (string? tag) (lower-case tag)
   :else tag))

(defn list-projects [{name "name" tag "tag"}]

  (if (not-any? true? [(nil? name) (string? name)])
    (make-error 400 "name must be specified 0 or 1 times")

    (let [results (db/list-projects name (lower-case-tag tag))]
      {:body {:results (apply vector (map attach-href (reverse results)))}})))