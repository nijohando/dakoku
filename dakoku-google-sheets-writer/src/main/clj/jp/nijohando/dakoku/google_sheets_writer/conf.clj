(ns jp.nijohando.dakoku.google-sheets-writer.conf
  (:require
    [mount.core :refer [defstate]]
    [environ.core :refer [env]]
    [jp.nijohando.failable :refer [flet fail guard]])
  (:import
    (java.time ZoneId)))

(defn- get-env
  [ckey]
  (or (env ckey)
      (fail (str (name ckey) " is not set."))))

(defn private-key-json-url
  []
  (get-env :private-key-json-url))

(defn delegated-user-email
  []
  (get-env :delegated-user-email))

(defn google-drive-base-path
  []
  (get-env :google-drive-base-path))

(defn google-spreadsheet-file-prefix
  []
  (get-env :google-spreadsheet-file-prefix))

(defn zone
  []
  (flet [zone (get-env :zone)]
    (ZoneId/of zone)))
