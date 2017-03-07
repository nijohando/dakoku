(ns jp.nijohando.dakoku.google-sheets-writer.lambda
  (:require
    [jp.nijohando.dakoku.google-sheets-writer.core :as core]
    [jp.nijohando.failable :refer [flet f-> fail guard]]
    [clojure.java.io :as jio]
    [cheshire.core :as json]
    [mount.core :as mount]
    [clojure.tools.logging :as log])
  (:gen-class
   :init init
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(defn -init
  []
  (mount/start)
  [[][]])

(defn -handleRequest [this in out context]
  (flet [r (jio/reader in)
        w (jio/writer out)
        req (json/parse-stream r true)
        _ (log/info "request:" req)
        notifications (map :Sns (:Records req))
        messages (map #(json/parse-string (:Message %) true) notifications)
        _ (log/info "messages:" messages)]
    (doseq [{:keys [subject event]} messages]
      (core/stamp subject event))))
