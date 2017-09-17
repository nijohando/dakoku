(defproject jp.nijohando.dakoku/aws-kinesis-firehose-adapter "1.0.0"
  :description "AWS Lambda for loading data into AWS Firehose"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.908"]
                 [org.clojure/core.async "0.3.443"]
                 [jp.nijohando/failable "0.1.1"]
                 [jp.nijohando/deferable "0.1.0"]]
  :source-paths ["src/main/clj", "src/dev/clj", "src/main/cljs"]
  :clean-targets [:target-path "out"]
  :aliases {"noderepl" ["run" "-m" "clojure.main" "src/dev/clj/tools/repl.clj"]
            "build" ["run" "-m" "clojure.main" "src/dev/clj/tools/build.clj"]
            "package" ["run" "-m" "clojure.main" "src/dev/clj/tools/package.clj"]
            "watch" ["run" "-m" "clojure.main" "src/dev/clj/tools/watch.clj"]})
