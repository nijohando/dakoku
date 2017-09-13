(ns jp.nijohando.dakoku.aws-kinesis-firehose-adapter.lambda
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [jp.nijohando.dakoku.aws-firehose-adapter.core :as core]
            [jp.nijohando.failable :refer [fail guard failure?] :refer-macros [flet f->]]
            [cljs.core.async :refer [promise-chan >! <!]]
            [cljs.nodejs :as nodejs]))

(defn notification->dakoku-event
  [notification]
  (let [[timestamp msg] ((juxt :Subject :Message) notification)]
    (merge (core/json-str->edn msg) {:timestamp (long timestamp)})))

(defn handler
  [sns-event context callback]
  (let [edn (js->clj sns-event :keywordize-keys true)
        _ (.log js/console (str "sns-event: " edn))
        notifications (map :Sns (:Records edn))
        dakoku-events (map notification->dakoku-event notifications)
        ch (core/stamp dakoku-events)]
    (if (failure? ch)
      (callback @ch)
      (go
        (let [[err data] (<! ch)]
          (callback err data))))))

(nodejs/enable-util-print!)
(aset js/exports "handler" handler)
