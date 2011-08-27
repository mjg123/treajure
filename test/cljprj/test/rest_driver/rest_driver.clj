(ns cljprj.test.rest-driver.rest-driver
  (:import com.github.restdriver.serverdriver.RestServerDriver)
  (:import com.github.restdriver.serverdriver.http.Header))


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

(defn header [name value]
  (Header. name value))

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

(defn- into-array-or-nil [vals]
  (when (seq vals)
    (into-array vals)))

;;; http functions

(defn GET [path & headers]
  (let [response (RestServerDriver/get (url path) (into-array-or-nil headers))]
    (to-response-map response)))

(defn POST [path & modifiers]
  (let [response (RestServerDriver/post (url path) (into-array-or-nil modifiers))]
    (to-response-map response)))

(defn PUT [path & modifiers]
  (let [response (RestServerDriver/put (url path) (into-array-or-nil modifiers))]
    (to-response-map response)))

(defn DELETE [path]
  (let [response (RestServerDriver/delete (url path) nil)]
    (to-response-map response)))