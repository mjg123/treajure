(ns cljprj.multi-complete
  (:require [clojure.contrib.json :as json]))

;; Various REST helpful stuffs

(defn- extract-accept-header [req _]
  (keyword (get-in req [:headers "accept"])))

(defmulti complete extract-accept-header)

(defmethod complete :application/json [_ {:keys [status headers body]}]
  {:status status
   :headers (assoc headers "content-type" "application/json")
   :body (json/json-str body)})


(defmethod complete :application/clojure [_ {:keys [status headers body]}]
  {:status status
   :headers (assoc headers "content-type" "application/clojure")
   :body (pr-str body)})


(defmethod complete :default [_ {:keys [status headers body]}]
  {:status status
   :headers (assoc headers "content-type" "text/plain")
   :body (str "The response is:\n\n  " body "\n\nWhy not try an accept header: application/json or application/clojure??")})


(defn req-body [req]
  (let [body-stream (req :body)
        content-type ((req :headers) "content-type")]
  
    (condp = (keyword content-type)
      :application/clojure (read-string (slurp body-stream))
      :application/json   (json/read-json (slurp body-stream))
      (slurp body-stream))))
