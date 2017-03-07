(ns repl
  (:require [jp.nijohando.dakoku.google-sheets-writer.core :as core]
            [jp.nijohando.dakoku.google-sheets-writer.auth :as auth]
            [jp.nijohando.dakoku.google-sheets-writer.drive :as drive]
            [jp.nijohando.dakoku.google-sheets-writer.sheets :as sheets]
            [mount.core :as mount]
            [clojure.tools.namespace.repl :as tn]))

(defn start
  []
  (mount/start #'jp.nijohando.dakoku.google-sheets-writer.auth/access-token))

(defn stop
  []
  (mount/stop))

(defn refresh
  []
  (stop)
  (tn/refresh))

(defn refresh-all
  []
  (stop)
  (tn/refresh-all))

(defn go
  "starts all states defined by defstate"
  []
  (start) :ready)

(defn reset
  "stops all states defined by defstate, reloads modified source files, and restarts the states"
  []
  (stop)
  (tn/refresh :after 'repl/go))

(mount/in-clj-mode)
