(ns cljprj.test.rest-driver.rest-driver
  (:import com.github.restdriver.serverdriver.RestServerDriver))

;; This is the first cut of what will hopefully become the clojure wrapper for rest-driver

(def
  ^{:doc "The base URL prepended to all requests"
    :private true}
  **base-url** (atom nil))

(defn
  ^{:doc "Set the base URL.  This will be prepended to all URLs"}
  set-base-url! [url]
  (reset! **base-url** url))

;; modifiers

(defn body [content type]
  (RestServerDriver/body content type))

;; helpers

(defn- url [path]
  (str @**base-url** path))

(defn- flatten-headers-list [headers]
  (apply hash-map
    (flatten
      (map #(list (keyword (. (. % getName) toLowerCase)) (. % getValue))
        headers))))

(defn- to-response-map [response]
  {:status (. response getStatusCode)
   :headers (flatten-headers-list (. response getHeaders))
   :body (. response getContent)})

;;; http functions

(defn http-get [path]
  (let [response (RestServerDriver/get (url path) nil)]
    (to-response-map response)))

(defn http-post [path & modifiers]
  (let [response (RestServerDriver/post (url path) (into-array modifiers))]
    (to-response-map response)))

(defn http-delete [path]
  (let [response (RestServerDriver/delete (url path) nil)]
    (to-response-map response)))