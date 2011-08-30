(ns cljprj.rest-lib
  (:use [clojure.contrib.string :only [split]])
  (:require [clojure.contrib.json :as json]))

;; Various REST helpful stuffs

(defn- extract-accept-header [req _]
  (keyword (get-in req [:headers "accept"])))

(defn write-with [f body]
  (if (nil? body)
    ""
    (f body)))

(defmulti complete extract-accept-header)

(defmethod complete :application/json [_ {:keys [status headers body]}]
  {:status status
   :headers (assoc headers "content-type" "application/json")
   :body (write-with json/json-str body)})


(defmethod complete :application/clojure [_ {:keys [status headers body]}]
  {:status status
   :headers (assoc headers "content-type" "application/clojure")
   :body (write-with pr-str body)})


(defmethod complete :default [_ {:keys [status headers body]}]
  {:status status
   :headers (assoc headers "content-type" "text/plain")
   :body (str "The response is:\n\n  " body "\n\nWhy not try an accept header: application/json or application/clojure??")})

(defn str-contains? [needle haystack]
  (not= -1 (.indexOf haystack needle)))

(defn extract-content-type [header]
  (if (nil? header)
    nil
    (if (str-contains? ";" header)
      (first (split #";" header))
      header)))

(defn req-body [req]
  (let [body-stream (req :body)
        content-type-with-charset ((req :headers) "content-type")
        content-type (extract-content-type content-type-with-charset)]

    (condp = (keyword content-type)
      :application/clojure (read-string (slurp body-stream))
      :application/json (json/read-json (slurp body-stream))
      (slurp body-stream))))
