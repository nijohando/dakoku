(ns tools
  (:require [cljs.repl]
            [cljs.repl.node]
            [cljs.build.api]
            [clojure.java.io :as jio]
            [jp.nijohando.deferable :refer [do* defer]])
  (:import (java.io File)
           (java.util.zip ZipEntry
                          ZipOutputStream)))

(def archive-file-name "dakoku-request-rewriter.zip")
(def cljs-src-dir "src/main/cljs")
(def target-dir "target")
(def cljs-out-dir "out")
(def cljs-main-js "index.js")
(def cljs-output-to (str cljs-out-dir "/" cljs-main-js))
(def cljs-source-map (str cljs-output-to ".map"))
(def cljs-npm-deps {})
(def cljs-compiler-opts (merge cljs-npm-deps
                               {:output-dir cljs-out-dir
                                :output-to cljs-output-to
                                :optimizations :advanced
                                :source-map cljs-source-map
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
      (append (.getName main-js) main-js))))

(defn repl
  []
  (cljs.repl/repl* (cljs.repl.node/repl-env)
                   (merge cljs-npm-deps
                          {:watch cljs-src-dir
                           :optimizations :simple
                           :output-dir "out"
                           :output-to cljs-output-to
                           :source-map cljs-source-map
                           :verbose true})))
