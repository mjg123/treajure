(defproject cljprj "1.0.0-SNAPSHOT"
  :description "Rooomy meeting room planner"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.6.4"]
                 [com.github.rest-driver/rest-server-driver "1.1.8"]
                 [ring/ring-core "0.3.8"]
                 [ring/ring-devel "0.3.8"]
                 [ring/ring-jetty-adapter "0.3.8"]]
  :dev-dependencies [[lein-ring "0.4.5"]
                     [lein-midje "1.0.0"]
                     [midje "1.1.1"]]
  :resources-path "resources"

  :ring {:handler cljprj.rest/app})

