(ns cljprj.rest
  (:use compojure.core)
  (:use cljprj.multi-complete)
  (:require
    [compojure.route :as route]
    [cljprj.core :as core]))

(def path-regex #"[^/]+") ; compojure will split paths on "." as well as "/"...

(defroutes cljprj-routes
  (GET "/ping" [:as req] "pong")

  (GET "/api/projects" [:as req]
    (complete req (core/list-projects)))

  (PUT "/api/projects" [:as req]
    (complete req (core/add-project (req-body req))))

  (GET ["/api/projects/:group-id/:artifact-id" :group-id path-regex :artifact-id path-regex]
    [group-id artifact-id :as req]
    (complete req (core/get-project group-id artifact-id)))

  (DELETE "/api/projects/:group-id/:artifact-id" [group-id artifact-id :as req]
    (complete req (core/rm-project group-id artifact-id)))

  (route/files "/" {:root "resources/www-root"})
  (route/not-found "404. Problem?"))