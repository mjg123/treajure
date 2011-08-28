(ns cljprj.core
  (:require [cljprj.persistence :as db]))

(defn valid-project?
  "Checks if the project contains the minimal amount of data to be valid"
  [prj-data]
  (and
    (map? prj-data)
    (contains? prj-data :name)
    (contains? prj-data :group-id)
    (contains? prj-data :artifact-id)))

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
    (do
      (db/add-project {:project prj :coords-idx (prj-coords prj)})
      {:status 201
       :headers {"location" (make-location-url prj)}
       :body {:message "yum, thanks"}})
    {:status 400
     :body {:error "uploaded project must have at least [:name :group-id :artifact-id]"}}))


(def no-such-project {:status 404 :body {:error "No such project"}})

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

(defn list-projects []
  (let [results (db/list-projects)]
    {:body {:results (map attach-href (reverse results))}}))