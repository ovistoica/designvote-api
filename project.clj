(defproject designvote-api "0.1.0-SNAPSHOT"
  :description "Designvote rest API"
  :url "https://api.designvote.io"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring "1.8.1"]
                 [integrant "0.8.0"]
                 [environ "1.2.0"]
                 [metosin/reitit "0.5.2"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [org.postgresql/postgresql "42.2.14"]
                 [clj-http "3.10.0"]
                 [ovotech/ring-jwt "1.2.5"]
                 [camel-snake-kebab "0.4.2"]
                 [com.zaxxer/HikariCP "3.4.5"]
                 [ring-cors "0.1.13"]
                 [buddy/buddy-core "1.9.0"] ]
  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev     {
                       :source-paths   ["dev/src"]
                       :resource-paths ["dev/resources"]
                       :dependencies   [[ring/ring-mock "0.4.0"] [integrant/repl "0.3.2"]]
                       }}
  :uberjar-name "designvote.jar")
