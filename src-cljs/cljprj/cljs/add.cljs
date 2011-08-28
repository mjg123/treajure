(ns cljprj.cljs.add
  (:require [goog.dom :as dom]
            [goog.date :as date]
            [goog.events :as events]
            [goog.net.XhrIo :as xhr]))

;;;;;;;;;;;;;;;;;;;;;;;;; DEBUG BUSINESS

(def log-elem (dom/getElement "log"))

(defn log [msg]
  (let [old-content (.innerHTML log-elem)]
    (set! (.innerHTML log-elem) (str (pr-str [(. (date/DateTime.) (toIsoString)) msg]) "<br/>" old-content))))


;;;;;;;;;;;;;;;;;;;;;;;;; GENERAL BUSINESS

(defn dom-val [elem-id]
  (.value (dom/getElement elem-id)))

(def whitespace-regex (js* "/\\s+/"))

(def upload-headers
  (doto (js-obj)
    (aset "Content-Type" "application/clojure")
    (aset "Accept" "application/clojure")))


;;;;;;;;; UPLOAD A NEW PROJECT BUSINESS

(defn submit-callback [e]
  (let [resp (aget e "target")
        status (. resp (getStatus))
        body (. resp (getResponseText))]
    (log [:submit-response status body])))

(defn project-from-form []
  {:name (dom-val "add-name")
   :group-id (dom-val "add-group-id")
   :artifact-id (dom-val "add-artifact-id")
   :author (dom-val "add-author")
   :latest-version (dom-val "add-latest-version")
   :source-url (dom-val "add-source-url")
   :readme-text (dom-val "add-readme-text")
   :tags (apply vector (.split (dom-val "add-tags") whitespace-regex))})

(defn submit-event []
  (xhr/send "/api/projects" submit-callback "PUT" (pr-str (project-from-form)) upload-headers))

(defn start-app []
  (events/listen (dom/getElement "add-submit")
    "click"
    submit-event))

(start-app)