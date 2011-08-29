(ns cljprj.cljs.add
  (:require [goog.dom :as dom]
            [goog.date :as date]
            [goog.events :as events]
            [goog.net.XhrIo :as xhr]))

;;;;;;;;;;;;;;;;;;;;;;;;; DEBUG BUSINESS

(def log-elem (dom/getElement "log"))

(defn log [msg]
  (let [old-content (.innerHTML log-elem)]
    (set! (.innerHTML log-elem) (str "user=> " (pr-str [(. (date/DateTime.) (toIsoString)) msg]) "<br/>" old-content))))


;;;;;;;;;;;;;;;;;;;;;;;;; GENERAL BUSINESS

(defn dom-val [elem-id]
  (.value (dom/getElement elem-id)))

(defn clear-elem [elem-id]
  (set! (.value (dom/getElement elem-id)) ""))

(def whitespace-regex (js* "/\\s+/"))

(def upload-headers
  (doto (js-obj)
    (aset "Content-Type" "application/clojure")
    (aset "Accept" "application/clojure")))

;;;;;;;;; SHOW AN INDIVIDUAL PROJECT BUSINESS

(defn load-individual-project-from [locn]
  (log (str "Loading from " locn)))


;;;;;;;;; UPLOAD A NEW PROJECT BUSINESS

(defn clear-add-form []
  (doall
    (map
      clear-elem
      ["add-name" "add-group-id" "add-artifact-id" "add-author"
       "add-latest-version" "add-source-url" "add-readme-text" "add-tags"])))

(defn project-upload-success [new-locn]
  (clear-add-form)
  (load-individual-project-from new-locn))

(defn submit-callback [e]
  (let [resp (aget e "target")
        status (. resp (getStatus))
        body (. resp (getResponseText))
        location (.getResponseHeader resp "location")]

    (log [:submit-response status body])
    (log location)

    (condp = status
      201 (project-upload-success location)
      400 (log "Upload failed :(")
      (log "Unexpected problem with upload"))))

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


;;;;;;;;;;;;;;;;;; START THE APP (BUSINESS)

(defn start-app []
  (log :startup)
  (events/listen (dom/getElement "add-submit")
    "click"
    submit-event))

(start-app)