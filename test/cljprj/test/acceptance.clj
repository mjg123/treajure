(ns cljprj.test.acceptance
  (:use [midje.sweet]
        [cljprj.test.cljprj-driver])
  (:require [cljprj.test.rest-driver.rest-driver :as drv]
            [clojure.contrib.json :as json]))

(def base-url (get (System/getenv) "BASE_URL" "http://localhost:8080"))

;;;; TESTS - NB these currently assume mongo to be EMPTY before you start

(facts "cljprj acceptance tests"

  (println (str "Using base url of " base-url))
  (drv/set-base-url! base-url)

  (clear-all-used-projects!)

  (fact "Ping pongs"
    (let [{status :status body :body} (drv/GET "/ping")]
      [status body] => [200 "pong"]))

  (fact "Missing page is correct 404"
    (let [{status :status body :body} (drv/GET "/missing-page")]
      [status body] => [404 "404. Problem?"]))

  (fact "Can upload a project and retrieve it again as application/clojure"
    (let [project (make-project 1)
          {upload-status :status {new-location :location} :headers} (add-project-clj project)
          {get-status :status body-str :body} (drv/GET new-location accept-clj)
          body (read-string body-str)]

      upload-status => 201
      get-status => 200
      (body :name) => (project :name)
      (body :group-id) => (project :group-id)
      (body :artifact-id) => (project :artifact-id)))

  (clear-all-used-projects!)

  (fact "Can upload a project and retrieve it again as application/json"
    (clear-all-used-projects!)
    (let [project (make-project 2)
          {upload-status :status {new-location :location} :headers} (add-project-json project)
          {get-status :status body-str :body} (drv/GET new-location accept-json)
          body (json/read-json body-str)]

      upload-status => 201
      get-status => 200
      (body :name) => (project :name)
      (body :group-id) => (project :group-id)
      (body :artifact-id) => (project :artifact-id)))

  (fact "Can upload a project and retrieve it again as with utf-8 charset specified"
    (clear-all-used-projects!)
    (let [project (make-unique-project)
          {upload-status :status {new-location :location} :headers} (add-project-clj-utf8 project)
          {get-status :status body-str :body} (drv/GET new-location accept-json)
          body (json/read-json body-str)]

      upload-status => 201
      get-status => 200
      (body :name) => (project :name)
      (body :group-id) => (project :group-id)
      (body :artifact-id) => (project :artifact-id)))

  (clear-all-used-projects!)

  (fact "Malformed project uploads fail - body"
    (let [{status :status} (drv/PUT "/api/projects")]
      status => 400))

  (fact "Malformed project uploads fail - name is mandatory"
    (let [{status :status} (add-project-clj (dissoc (make-project "xx") :name))]
      status => 400))

  (fact "Malformed project uploads fail - group-id is mandatory"
    (let [{status :status} (add-project-clj (dissoc (make-project "xx") :group-id))]
      status => 400))

  (fact "Malformed project uploads fail - artifact-id is mandatory"
    (let [{status :status} (add-project-clj (dissoc (make-project "xx") :artifact-id))]
      status => 400))

  (fact "DELETE a project after upload means you can't GET it any more (or DELETE it)"
    (let [project (make-project 3)
          {upload-status :status {new-location :location} :headers} (add-project-clj project)
          {get-status :status} (drv/GET new-location accept-clj)]

      upload-status => 201
      get-status => 200

      (let [{delete-status :status} (drv/DELETE new-location)
            {get-status :status} (drv/GET new-location accept-clj)]

        delete-status => 204
        get-status => 404

        (let [{redelete-status :status} (drv/DELETE new-location)]

          redelete-status => 404))))

  (fact "we can GET a list of all projects - even if it's an empty list"
    (clear-all-used-projects!)
    (let [{status :status body-str :body} (drv/GET "/api/projects" accept-clj)
          {results :results} (read-string body-str)]
      status => 200
      results => []))

  (fact "we can GET a list of all projects - with one item"
    (clear-all-used-projects!)
    (let [project (make-project "one")
          _ (add-project-clj project)
          {status :status body-str :body} (drv/GET "/api/projects" accept-clj)
          {results :results} (read-string body-str)
          result (first results)]
      status => 200
      (result :name) => (project :name)))

  (fact "we can GET a list of all projects - with more than one item"
    (clear-all-used-projects!)
    (add-project-clj (make-project "one"))
    (add-project-clj (make-project "two"))
    (add-project-clj (make-project "th3"))

    (let [{status :status body-str :body} (drv/GET "/api/projects" accept-clj)
          {results :results} (read-string body-str)]
      status => 200
      (count results) => 3))


  (fact "Projects in the list of projects have an href which points at their page"
    (clear-all-used-projects!)

    (let [project (make-project "one")
          _ (add-project-clj project)
          {status :status body-str :body} (drv/GET "/api/projects" accept-clj)
          {results :results} (read-string body-str)
          {href :href} (first results)]

      status => 200
      (nil? href) => false

      (let [{get-status :status body :body} (drv/GET href accept-json)
            prj-body (json/read-json body)]

        get-status => 200
        (project :name) => (prj-body :name)
        (prj-body :href) => nil)))

  (fact "Projects data items are constrained"
    (clear-all-used-projects!)
    (let [project (assoc (make-full-project "five") :no-good-extra "WHATEVERS")
          {upload-status :status {new-location :location} :headers} (add-project-clj project)
          {get-status :status body-str :body} (drv/GET new-location accept-clj)
          body (read-string body-str)]

      upload-status => 201
      get-status => 200

      (keys body) => (in-any-order (keys (make-full-project "five")))))

  (fact "list-project filtering error cases"
    ((drv/GET "/api/projects?name=foo&name=bar") :status) => 400)

  (fact "filtering by name"
    (clear-all-used-projects!)

    (let [expected (make-project "matthew")]
    
      (add-project-clj expected)
      (add-project-clj (make-project "andrew"))

      (let [resp (drv/GET "/api/projects?name=matthew" accept-clj)
            body (read-string (resp :body))]

        (resp :status) => 200
        (count (body :results)) => 1
        (get-in body [:results 0 :name]) => (expected :name))))


  (clear-all-used-projects!))
