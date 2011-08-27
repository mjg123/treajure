(ns cljprj.rest
  (:use compojure.core)
  (:use cljprj.multi-complete)
  (:use ring.middleware.stacktrace)
  (:require
    [compojure.route :as route]
    [compojure.handler :as handler]))

(defroutes cljprj-routes
  (GET "/ping" [] "pong")
  (GET "/error" [] {:status 500 :body "Oh NOES!"})

  (GET "/debug" [:as req]
    (complete req
      {:body {:simple "Object" :with {:nested "map" :and [:a :long :list]}}}))

  (route/not-found "404. Problem?"))

(def app
  (->
    (handler/site cljprj-routes)
    (wrap-stacktrace)))