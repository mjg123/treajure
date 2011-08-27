(ns cljprj.test.acceptance
  (:use [midje.sweet])
  (:require [cljprj.test.rest-driver.rest-driver :as drv]
            [clojure.contrib.json :as json]))

(def base-url (get (System/getenv) "BASE_URL" "http://localhost:8080"))

(def good-project-1 {:name "Test project 1" :group-id "cljprj-test-group-id-1" :artifact-id "cljprj-test-artifact-id-1"})
(def good-project-2 {:name "Test project 2" :group-id "cljprj-test-group-id-2" :artifact-id "cljprj-test-artifact-id-2"})

(def accept-clj (drv/header "accept" "application/clojure"))
(def accept-json (drv/header "accept" "application/json"))

(defn add-project-clj [project]
  (drv/PUT
    "/api/projects"
    (drv/body project "application/clojure")))

(defn add-project-json [project]
  (drv/PUT
    "/api/projects"
    (drv/body project "application/json")))

(facts "cljprj acceptance tests"

  (println (str "Using base url of " base-url))
  (drv/set-base-url! base-url)

  (fact "Ping pongs"
    (let [{status :status body :body} (drv/GET "/ping")]
      [status body] => [200 "pong"]))

  (fact "Missing page is correct 404"
    (let [{status :status body :body} (drv/GET "/missing-page")]
      [status body] => [404 "404. Problem?"]))

  (fact "Can upload a project and retrieve it again as application/clojure"
    (let [{upload-status :status {new-location :location} :headers} (add-project-clj (pr-str good-project-1))
          {get-status :status body-str :body} (drv/GET new-location accept-clj)
          body (read-string body-str)]

      upload-status => 201
      get-status => 200
      (body :name) => (good-project-1 :name)
      (body :group-id) => (good-project-1 :group-id)
      (body :artifact-id) => (good-project-1 :artifact-id)))

  (fact "Can upload a project and retrieve it again as application/json"
    (let [{upload-status :status {new-location :location} :headers} (add-project-json (json/json-str good-project-2))
          {get-status :status body-str :body} (drv/GET new-location accept-json)
          body (json/read-json body-str)]

      upload-status => 201
      get-status => 200
      (body :name) => (good-project-2 :name)
      (body :group-id) => (good-project-2 :group-id)
      (body :artifact-id) => (good-project-2 :artifact-id)))

  (fact "Malformed project uploads fail - body"
    (let [{status :status} (drv/PUT "/api/projects")]
      status => 400))

  (fact "Malformed project uploads fail - name is mandatory"
    (let [{status :status} (add-project-clj (pr-str (dissoc good-project-1 :name)))]
      status => 400))

  (fact "Malformed project uploads fail - group-id is mandatory"
    (let [{status :status} (add-project-clj (pr-str (dissoc good-project-1 :group-id)))]
      status => 400))

  (fact "Malformed project uploads fail - artifact-id is mandatory"
    (let [{status :status} (add-project-clj (pr-str (dissoc good-project-1 :artifact-id)))]
      status => 400)))