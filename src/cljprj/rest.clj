(ns cljprj.rest
  (:use compojure.core)
  (:require
    [compojure.route :as route]
    [compojure.handler :as handler]))

(defroutes cljprj-routes
  (GET "/ping" [] "pong")
  (GET "/error" [] {:status 500 :body "Oh NOES!"})

  (route/not-found "Problem?"))

(def app (handler/site cljprj-routes))