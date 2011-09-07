(ns treajure.web
  (:use ring.adapter.jetty)
  (:use ring.middleware.stacktrace)
  (:require [treajure.rest :as rest]
            [compojure.handler :as handler]))

(def app
  (->
    (handler/site rest/treajure-routes)
    (wrap-stacktrace)))

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
    (println (str "Commencing the fun on port " port))
    (run-jetty app {:port port})))
