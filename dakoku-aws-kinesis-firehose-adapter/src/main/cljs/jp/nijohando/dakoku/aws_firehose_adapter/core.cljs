(ns jp.nijohando.dakoku.aws-kinesis-firehose-adapter.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jp.nijohando.failable :refer-macros [flet f-> fail guard]]
            [aws-sdk :as aws]
            [cljs.core.async :refer [promise-chan >! <!]]))

(defn json-str->edn
  [json-str]
  (-> (.parse js/JSON json-str)
      (js->clj :keywordize-keys true)))

(defn edn->json-str
  [clj]
  (->> clj
       clj->js
       (.stringify js/JSON)))

(defn- new-client
  [opts]
  (new aws/Firehose (js->clj opts)))

(defn- put-records
  [client delivery-stream-name events]
  (let [ch (promise-chan)
        params {:DeliveryStreamName delivery-stream-name
                :Records (map (fn [x] {:Data (edn->json-str x)}) events)}]
    (.putRecordBatch client (clj->js params) (fn [err data] (go (>! ch [err data]))))
    ch))

(defn stamp [events]
  (flet [client (new-client {:apiVersion "2015-08-04"})]
    (put-records client "dakoku" events)))
