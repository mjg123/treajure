(ns cljprj.core
  (:require [cljprj.persistence :as db]))

(defn valid-project? [prj-data]
  true)

(defn make-location [prj]
  (str "/api/projects/" (prj :group-id) "/" (prj :artifact-id)))

(defn- coords [gid aid]
  (str gid " $$$ " aid))

(defn- prj-coords [prj]
  (coords (prj :group-id) (prj :artifact-id)))

(defn add-project [prj]
  (when (valid-project? prj)
    (db/add-project {:project prj :coords (prj-coords prj)})
    {:status 201
     :headers {"location" (make-location prj)}
     :body "yum, thanks"}))

(defn get-project [gid aid]
  (let [result (db/get-project (coords gid aid))]
    {:body result}))