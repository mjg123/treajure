(ns treajure.test.acceptance
  (:use [midje.sweet]
        [treajure.test.treajure-driver])
  (:require [treajure.test.rest-driver.rest-driver :as drv]
            [clojure.contrib.json :as json]))

(def base-url (get (System/getenv) "BASE_URL" "http://localhost:8080"))

;;;; TESTS - NB these currently assume mongo to be EMPTY before you start

(facts "treajure acceptance tests"

  (fact "security token is available"
    (when (nil? (System/getenv "TREAJURE_TOKEN"))
      (println "HEY YOU: You have to set the environment var TREAJURE_TOKEN"))
    (System/getenv "TREAJURE_TOKEN") => #(not (nil? %)))

  (println (str "Using base url of " base-url))
  (drv/set-base-url! base-url)

  (fact "Ping pongs"
    (let [{status :status body :body} (drv/GET "/ping")]
      [status body] => [200 "pong"]))

  (fact "Missing page is correct 404"
    (let [{status :status body :body} (drv/GET "/missing-page")]
      [status body] => [404 "404. Problem?"]))

  (fact "Can upload a project and retrieve it again as application/clojure"
    (clear-all-used-projects!)
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

      (let [{delete-status :status} (clear-project project)
            {get-status :status} (drv/GET new-location accept-clj)]

        delete-status => 204
        get-status => 404

        (let [{redelete-status :status} (clear-project project)]

          redelete-status => 204))))

  (fact "DELETE a project after upload needs the token"
    (let [project (make-unique-project)
          {upload-status :status {new-location :location} :headers} (add-project-clj project)]

      upload-status => 201

      (let [{delete-status :status} (clear-project-without-token project)
            {get-status :status} (drv/GET new-location accept-clj)]

        delete-status => 403
        get-status => 200 )))

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

  (fact "filtering by tags"
    (clear-all-used-projects!)

    (let [expected-1 (assoc (make-project "matthew") :tags ["t1" "t2"])
          expected-2 (assoc (make-project "andrew") :tags ["t2" "t3"])]

      (add-project-clj expected-1)
      (add-project-clj expected-2)

      (let [resp (drv/GET "/api/projects?tag=t2" accept-clj)
            body (read-string (resp :body))]
        (resp :status) => 200
        (count (body :results)) => 2)

      (let [resp (drv/GET "/api/projects?tag=t1" accept-clj)
            body (read-string (resp :body))]
        (resp :status) => 200
        (count (body :results)) => 1
        (get-in body [:results 0 :name]) => (expected-1 :name))

      (let [resp (drv/GET "/api/projects?tag=t2&tag=t3" accept-clj)
            body (read-string (resp :body))]
        (resp :status) => 200
        (count (body :results)) => 1
        (get-in body [:results 0 :name]) => (expected-2 :name))))

  (fact "Can't add the same project twice"
    (clear-all-used-projects!)

    (let [project (make-unique-project)
          add-1 (add-project-clj project)
          add-2 (add-project-clj project)]

      (add-1 :status) => 201
      (add-2 :status) => 409))

  (fact "search results are limited"
        (clear-all-used-projects!)
        (let [projects (vec (map #(assoc (make-project %) :tags ["limit-tag"]) (range 0 11)))]
          (doseq [prj projects] (add-project-clj prj))

          (let [resp (drv/GET "/api/projects?tag=limit-tag" accept-clj)
                body (read-string (resp :body))]
            (count (body :results)) => 10
            (doseq [n (range 1 11)]
              (get-in body [:results (- n 1) :name]) => (:name (nth projects (- 11 n)))))))

  (clear-all-used-projects!))
