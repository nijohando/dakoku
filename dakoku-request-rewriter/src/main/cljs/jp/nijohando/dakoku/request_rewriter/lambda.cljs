(ns jp.nijohando.dakoku.request-rewriter.lambda
  (:require [cljs.nodejs :as nodejs]))

(defn- now
  []
  (.now js/Date))

(defn- rewrite
  [event]
  (-> (get-in event [:Records 0 :cf :request])
      (update-in [:headers :x-dakoku-timestamp] concat [{:key "X-Dakoku-Timestamp" :value (str (now))}])))

(defn handler
  [event context callback]
  (->> (js->clj event :keywordize-keys true)
       (rewrite)
       (clj->js)
       (callback nil)))

(nodejs/enable-util-print!)
(aset js/exports "handler" handler)
