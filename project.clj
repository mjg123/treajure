(defproject treajure "1.0.0-SNAPSHOT"
  :description "treajure - a clojure library service"
  :dependencies [[org.clojure/clojure "1.3.0"]
		 [org.clojure/data.json "0.1.1"]
                 [compojure "0.6.5"]
                 [ring/ring-core "0.3.11"]
                 [ring/ring-devel "0.3.11"]
                 [ring/ring-jetty-adapter "0.3.11"]
                 [congomongo "0.1.7-SNAPSHOT"]]
  :dev-dependencies [[lein-ring "0.4.6"]
                     [lein-midje "1.0.3"]
                     [com.github.rest-driver/rest-server-driver "1.1.8"]
                     [midje "1.3-alpha2"]]
  :resources-path "resources"

  :ring {:handler treajure.web/app})

