(ns jp.nijohando.dakoku.google-sheets-writer.core
  (:require
    [jp.nijohando.dakoku.google-sheets-writer.drive :as drive]
    [jp.nijohando.dakoku.google-sheets-writer.sheets :as sheets]
    [jp.nijohando.dakoku.google-sheets-writer.conf :as conf]
    [jp.nijohando.failable :refer [flet f-> fail guard]]
    [clojure.string :as s]
    [clojure.tools.logging :as log]
    [environ.core :refer [env]])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)))

(defn- get-spreadsheet
  []
  (flet [base-path (conf/google-drive-base-path)
         file-prefix (conf/google-spreadsheet-file-prefix)
         year (str (java.time.Year/now))
         file (sheets/get-or-create-file base-path (format "%s%s", file-prefix, year))
         spreadsheet-id (:id file)
         spreadsheet (sheets/get-spreadsheet spreadsheet-id)]
    spreadsheet))

(defn- get-current-sheet
  [spreadsheet]
  (flet [spreadsheet-id (:spreadsheetId spreadsheet)
         sheets (:sheets spreadsheet)
         month (->> (java.time.YearMonth/now)
                    (.getMonth)
                    (.getValue)
                    (format "%02d"))
         sheet (or (-> (some #(when (= (get-in % [:properties :title]) month)
                                %) sheets)
                       :properties)
                   (sheets/create-sheet spreadsheet-id {:title month
                                                        :gridProperties {:rowCount (int 1)}}))]
    sheet))

(defn stamp
  [subject {:keys [name modifiers] :as event}]
  (-> (flet [spreadsheet (get-spreadsheet)
             spreadsheet-id (:spreadsheetId spreadsheet)
             sheet (get-current-sheet spreadsheet)
             sheet-id (:sheetId sheet)
             zone (conf/zone)
             now (LocalDateTime/now zone)
             fnow (.format now DateTimeFormatter/ISO_LOCAL_DATE_TIME)
             stamp-data [fnow subject name (s/join "," modifiers)]
             _ (sheets/append-row spreadsheet-id sheet-id stamp-data)]
        stamp-data)
      (guard "Failed to stamp")))

