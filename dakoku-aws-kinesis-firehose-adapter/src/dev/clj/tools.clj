(ns tools
  (:require [cljs.repl]
            [cljs.repl.node]
            [cljs.build.api]
            [clojure.java.io :as jio]
            [jp.nijohando.deferable :refer [do* defer]])
  (:import (java.io File)
           (java.util.zip ZipEntry
                          ZipOutputStream)))

(def archive-file-name "dakoku-aws-kinesis-firehose-adapter.zip")
(def cljs-src-dir "src/main/cljs")
(def target-dir "target")
(def cljs-out-dir "out")
(def cljs-main-js "index.js")
(def cljs-output-to (str cljs-out-dir "/" cljs-main-js))
(def cljs-npm-deps {:npm-deps {:aws-sdk "2.94.0"}
                    :install-deps true})
(def cljs-compiler-opts (merge cljs-npm-deps
                               {:output-dir cljs-out-dir
                                :output-to cljs-output-to
                                :optimizations :simple
                                :target :nodejs
                                :verbose true}))

(defn build
  []
  (->> cljs-compiler-opts
       (cljs.build.api/build cljs-src-dir)))

(defn watch
  []
  (->> cljs-compiler-opts
       (cljs.build.api/watch cljs-src-dir)))

(defn package
  []
  (build)
  (do*
    (let [zip (ZipOutputStream. (jio/output-stream (str target-dir "/" archive-file-name)))
           _ (defer (.close zip))
           main-js (jio/file cljs-output-to)
           append (fn [^String entry-name ^File f]
                    (.putNextEntry zip (ZipEntry. entry-name))
                    (jio/copy f zip)
                    (.closeEntry zip))]
      (append (.getName main-js) main-js)
      (comment (doseq [f (file-seq (jio/file "node_modules")) :when (.isFile f)]
        (append (.getPath f) f))))))

(defn repl
  []
  (cljs.repl/repl* (cljs.repl.node/repl-env)
                   (merge cljs-npm-deps
                          {:watch cljs-src-dir
                           :optimizations :simple
                           :output-dir "out"
                           :output-to cljs-output-to
                           :verbose true})))
