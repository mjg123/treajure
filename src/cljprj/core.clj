(ns cljprj.core
  (:require [cljprj.persistence :as db]))

(defn valid-project? [prj-data]
  (and
    (map? prj-data)
    (not (nil? (prj-data :name)))
    (not (nil? (prj-data :group-id)))
    (not (nil? (prj-data :artifact-id)))))

(defn make-location [prj]
  (str "/api/projects/" (prj :group-id) "/" (prj :artifact-id)))

(defn- coords
  "Create the value to be used in the get-field for mongo.
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
       :headers {"location" (make-location prj)}
       :body {:message "yum, thanks"}})
    {:status 400
     :body {:error "uploaded project must have at least [:name :group-id :artifact-id]"}}))


(def no-such-project {:status 404 :body {:error "No such project"}})

(defn get-project [gid aid]
  (let [result (db/get-project (coords gid aid))]
    (if result
      {:body result}
      no-such-project)))

(defn rm-project [gid aid]
  (let [result (db/rm-project (coords gid aid))]
    (if result
      {:status 204 :body nil}
      no-such-project)))

(defn attach-href [prj]
  (assoc prj :href (make-location prj)))

(defn list-projects []
  (let [results (db/list-projects)]
    {:body {:results (map attach-href (reverse results))}}))