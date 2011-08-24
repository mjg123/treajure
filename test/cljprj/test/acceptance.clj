(ns cljprj.test.acceptance
  (:use [midje.sweet])
  (:require [cljprj.test.rest-driver.rest-driver :as drv]))

(facts "cljprj acceptance tests"

  (drv/set-base-url! "http://localhost:8080")

  (fact "Ping pongs"
    (let [{status :status body :body} (drv/http-get "/ping")]
      [status body] => [200 "pong"]))

  (fact "Error is an error"
    (let [{status :status body :body} (drv/http-get "/error")]
      [status body] => [500 "Oh NOES!"]))

  (fact "Missing page is correct 404"
    (let [{status :status body :body} (drv/http-get "/missing-page")]
      [status body] => [404 "Problem?"])))