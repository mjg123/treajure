(ns treajure.rest
  (:use compojure.core)
  (:use treajure.rest-lib)
  (:require
    [compojure.route :as route]
    [treajure.core :as core]))

(def path-regex #"[^/]+") ; compojure will split paths on "." as well as "/"...

(def auth-token (get (System/getenv) "TREAJURE_TOKEN" (str (java.util.UUID/randomUUID))))
(def auth-error {:status 403 :body "unauthorized"})
(println (str "Auth token is <<" auth-token ">>"))

(defroutes treajure-routes
  (GET "/ping" [:as req] "pong")

  (GET "/api/projects" [:as req]
    (complete req
      (core/list-projects (select-keys (req :query-params) ["name" "tag"]))))

  (PUT "/api/projects" [:as req]
    (complete req (core/add-project (req-body req))))

  (GET ["/api/projects/:group-id/:artifact-id" :group-id path-regex :artifact-id path-regex]
    [group-id artifact-id :as req]
    (complete req (core/get-project group-id artifact-id)))

  (DELETE ["/api/projects/:group-id/:artifact-id" :group-id path-regex :artifact-id path-regex]
    [group-id artifact-id :as req]
    (if (= (get-in req [:query-params "token"]) auth-token)
      (complete req (core/rm-project group-id artifact-id))
      (do
        (println (str "Auth token is <<" auth-token ">>"))
        (complete req auth-error))))


  (route/files "/" {:root "resources/www-root"})
  (route/not-found "404. Problem?"))