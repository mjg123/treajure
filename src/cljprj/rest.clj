(ns cljprj.rest
  (:use compojure.core)
  (:use cljprj.multi-complete)
  (:require
    [compojure.route :as route]
    [cljprj.core :as core]))

(defroutes cljprj-routes
  (GET "/ping" [] "pong")

  (PUT "/api/projects" [:as req]
    (complete req (core/add-project (req-body req))))

  (GET "/api/projects/:group-id/:artifact-id" [group-id artifact-id :as req]
    (complete req (core/get-project group-id artifact-id)))


  (route/not-found "404. Problem?"))