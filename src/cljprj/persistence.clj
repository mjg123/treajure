(ns cljprj.persistence
  (:use somnium.congomongo))

;; copied from http://thecomputersarewinning.com/post/clojure-heroku-noir-mongo
(defn- split-mongo-url [url]
  "Parses mongodb url from heroku, eg. mongodb://user:pass@localhost:1234/db"
  (let [matcher (re-matcher #"^.*://(.*?):(.*?)@(.*?):(\d+)/(.*)$" url)] ;; Setup the regex.
    (when (.find matcher) ;; Check if it matches.
      (zipmap [:match :user :pass :host :port :db] (re-groups matcher))))) ;; Construct an options map.

;; MONGOHQ_URL is provided automagically on Heroku
(def default-mongo-connection "mongodb://cljprj:cljprj@localhost:27017/cljprj")

(let [config (split-mongo-url (get (System/getenv) "MONGOHQ_URL" default-mongo-connection))]
  (mongo! :db (:db config) :host (:host config) :port (Integer. (:port config)))
  (authenticate (:user config) (:pass config))
  (add-index! :projects [:coords-idx]))



(defn add-project [project-data]
  (insert! :projects project-data))

(defn get-project [coords]
  (let [result (fetch-one :projects :where {:coords-idx coords})]
    (:project result)))

(defn rm-project [coords]
  (let [result (fetch-one :projects :where {:coords-idx coords})]
    (when (not (nil? result))
      (destroy! :projects result))
    result))

(defn list-projects [name]
  (map :project (fetch :projects)))
