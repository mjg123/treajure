(ns cljprj.cljs.add
  (:require [cljs.reader :as reader]
            [goog.dom :as dom]
            [goog.date :as date]
            [goog.events :as events]
            [goog.Uri :as uri]
            [goog.net.XhrIo :as xhr]
            [clojure.string :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;; DEBUG BUSINESS

(def log-elem (dom/getElement "log"))

(defn log [msg]
  (let [old-content (.innerHTML log-elem)]
    (set! (.innerHTML log-elem) (str "user=> " (pr-str [(. (date/DateTime.) (toIsoString)) msg]) "<br/>" old-content))))


;;;;;;;;;;;;;;;;;;;;;;;;; GENERAL BUSINESS

(def whitespace-regex (js* "/\\s+/"))

(defn dom-val [elem-id]
  (s/trim (.value (dom/getElement elem-id))))

(defn clear-elem [elem-id]
  (set! (.value (dom/getElement elem-id)) ""))

(defn set-html [elem-id val]
  (set! (.innerHTML (dom/getElement elem-id)) val))

(def ajacs-headers
  (doto (js-obj)
    (aset "Content-Type" "application/clojure")
    (aset "Accept" "application/clojure")))

(defn msg
  "This is for when a message needs showing to the user"
  [type message]
  ;; at the moment, just log to the error thingy
  (condp = type
    :error ((js* "alert") (str "Error: " message))
    (log [:unknown message])))

(defn un-xhr [e]
  (let [resp (aget e "target")]
    {:status (. resp (getStatus))
     :body (reader/read-string (. resp (getResponseText)))
     :headers (. resp (getAllResponseHeaders))}))

(defn add-uri-parameter [uri p-name p-val]
  (if (not= "" p-val)
    (.setParameterValue uri p-name p-val)
    uri))

(defn add-uri-parameters [uri p-name p-vals]
  (if (seq p-vals)
    (.setParameterValues uri p-name (apply array p-vals))
    uri))

;;;;;;;;; SHOW AN INDIVIDUAL PROJECT BUSINESS

(defn load-callback [e]
  (let [resp (un-xhr e)
        prj (resp :body)]

    (set-html "show-name" (prj :name))
    (set-html "show-group-id" (prj :group-id))
    (set-html "show-artifact-id" (prj :artifact-id))
    (set-html "show-version" (prj :version))
    (set-html "show-author" (prj :author))
    (set-html "show-source-url" (prj :source-url))
    (set-html "show-tags" (str "[" (apply str (interpose " " (prj :tags))) "]"))
    (set-html "show-readme-text" (prj :readme-text))))

(defn load-individual-project-from [location]
  (let [uri (goog.Uri. location)]
    (log (str "Loading from " uri))
    (xhr/send uri load-callback "GET" nil ajacs-headers)))

;;;;;;;;; UPLOAD A NEW PROJECT BUSINESS

(defn clear-add-form []
  (doall
    (map
      clear-elem
      ["add-name" "add-group-id" "add-artifact-id" "add-author"
       "add-version" "add-source-url" "add-readme-text" "add-tags"])))

(defn project-upload-success [new-locn]
  (clear-add-form)
  (load-individual-project-from new-locn))

(defn submit-callback [e]
  (let [resp (un-xhr e)
        location (.getResponseHeader (aget e "target") "location")] ;; TODO un-xhr should take care of this.  It's a capitalization problem I think.

    (condp = (resp :status)
      201 (project-upload-success location)
      400 (msg :error (str "Send failed: " (get-in resp [:body :error])))
      409 (msg :error (str "Send failed: " (get-in resp [:body :error])))
      (msg :error (str "Unexpected problem with send: " (get-in resp [:body :error]))))))

(defn project-from-form []
  {:name (dom-val "add-name")
   :group-id (dom-val "add-group-id")
   :artifact-id (dom-val "add-artifact-id")
   :author (dom-val "add-author")
   :version (dom-val "add-version")
   :source-url (dom-val "add-source-url")
   :readme-text (dom-val "add-readme-text")
   :tags (apply vector (remove #(= % "") (.split (dom-val "add-tags") whitespace-regex)))})

(defn submit-event []
  (xhr/send "/api/projects" submit-callback "PUT" (pr-str (project-from-form)) ajacs-headers))


;;;;;;;;;;;;;;;;;; GET SEARCH RESULTS BUSINESS

(defn clear-search-results! []
  (set-html "find-projects-results" ""))

(defn get-search-params []
  {:name (dom-val "search-name")
   :tags (apply vector (remove #(= % "") (.split (dom-val "search-tags") whitespace-regex)))
   :sort (dom-val "search-sort")})

(defn make-search-url [search-params]
  (-> (goog.Uri. "/api/projects")
    (add-uri-parameter "name" (search-params :name))
    (add-uri-parameters "tag" (search-params :tags))))

(defn make-result-dom [prj]
  (let [div (dom/createDom "div" "find-projects-item")]
    (events/listen div "click" #(load-individual-project-from (prj :href)))

    (.appendChild div (dom/createDom "span" {} "{ "))
    (.appendChild div (dom/createDom "span" "keyword" ":name "))
    (.appendChild div (dom/createDom "span" {} (prj :name)))
    (.appendChild div (dom/createDom "span" "keyword" ":tags "))

    (.appendChild div (dom/createDom "span" {} "[ "))

    (doseq [tag (prj :tags)]
      (.appendChild div (dom/createDom "span" "tag" (str tag " "))))

    (.appendChild div (dom/createDom "span" {} " ]"))

    (.appendChild div (dom/createDom "span" {} " }"))
    div))

(defn show-result! [prj]
  (let [new-div (make-result-dom prj)]
    (.appendChild (dom/getElement "find-projects-results") new-div)))

(defn show-results-callback [e]
  (let [resp (un-xhr e)]
    (doall (map show-result! (get-in resp [:body :results])))))

(defn do-search []
  (clear-search-results!)
  (let [search-params (get-search-params)
        search-url (make-search-url search-params)]
    (log (str "Searching with " search-url))
    (xhr/send search-url show-results-callback "GET" nil ajacs-headers)))


;;;;;;;;;;;;;;;;;; START THE APP (BUSINESS)

(defn start-app []
  (log :startup)
  (do-search)

  (events/listen (dom/getElement "add-submit")
    "click"
    submit-event)

  (events/listen (dom/getElement "search-submit")
    "click"
    do-search))

(start-app)