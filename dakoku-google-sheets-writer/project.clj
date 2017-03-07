(defproject jp.nijohando.dakoku/google-sheets-writer "1.0.0"
  :description "AWS Lambda to write google spreadsheets"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :scope "provided"]
                 [cheshire "5.7.0"]
                 [mount "0.1.11"]
                 [environ "1.1.0"]
                 [buddy/buddy-core "1.2.0"]
                 [buddy/buddy-sign "1.4.0"]
                 [clj-http "3.4.1"]
                 [jp.nijohando/failable "0.1.0"]
                 [jp.nijohando/deferable "0.1.0"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.86" :scope "provided"]
                 [com.amazonaws/aws-lambda-java-log4j "1.0.0" :scope "provided"]]
  :exclusions [[org.clojure/clojurescript]]
  :plugins [[lein-cljfmt "0.5.6"]]
  :source-paths ["src/main/clj"]
  :resource-paths ["src/main/resources"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.0-alpha3"]]
                   :source-paths ["src/dev/clj"]
                   :repl-options {:init-ns repl}}
             :prod {:source-paths ["src/prod/clj"]
                    :resource-paths ["src/prod/resources"]
                    :aot :all}})
