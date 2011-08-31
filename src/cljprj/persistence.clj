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
  (add-index! :projects [:coords-idx] :unique true))

(defn- coords
  "Create the value to be used in the mongo index.
  As get is always by gid/aid we just need those two things."
  ([gid aid]
    (str gid " $$$ " aid))
  ([prj]
    (coords (prj :group-id) (prj :artifact-id))))

(defn add-project [project]
  (insert! :projects {:coords-idx (coords project) :project project}))

(defn get-project [gid aid]
  (let [result (fetch-one :projects :where {:coords-idx (coords gid aid)})]
    (:project result)))

(defn rm-project [gid aid]
  (let [result (fetch-one :projects :where {:coords-idx (coords gid aid)})]
    (when (not (nil? result))
      (destroy! :projects result))
    result))

(defn- assoc-name-query [where-clause name]
  (cond
    (string? name) (assoc where-clause :project.name (re-pattern (str name "(?i)")))
    :else where-clause))

(defn- assoc-tags-query [where-clause tag]
  (cond
    (string? tag) (assoc where-clause :project.tags tag)
    (vector? tag) (assoc where-clause :project.tags {:$all tag})
    :else where-clause))

(defn list-projects [name tag]

  (let [where-clause
        (-> {}
          (assoc-name-query name)
          (assoc-tags-query tag))]

    (map :project (fetch :projects :where where-clause))))

