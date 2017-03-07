(ns jp.nijohando.dakoku.google-sheets-writer.sheets
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as s]
    [clj-http.client :as client]
    [jp.nijohando.dakoku.google-sheets-writer.auth :as auth]
    [jp.nijohando.dakoku.google-sheets-writer.drive :as drive]
    [jp.nijohando.failable :refer [flet f-> fail]]))


(defn get-or-create-file
  [dir file-name]
  (flet [folder (drive/get-folder dir)
         file (->> {:query (format "name = '%s' and '%s' in parents and trashed = false"
                                   file-name
                                   (:id folder))}
                   (drive/find-files)
                   (first))]
    (or file
        (-> (drive/create-file {:name file-name
                                :parents [(:id folder)]
                                :mimeType "application/vnd.google-apps.spreadsheet"})))))



(defn get-spreadsheet
  [spreadsheet-id]
  (flet [response (client/get (format "https://sheets.googleapis.com/v4/spreadsheets/%s", spreadsheet-id)
                              {:oauth-token auth/access-token
                               :as :json})]
    (:body response)))

(defn batch-update
  ([spreadsheet-id requests]
   (batch-update spreadsheet-id requests {}))
  ([spreadsheet-id requests {:keys [includeSpreadsheetInResponse
                                    responseRanges
                                    responseIncludeGridData]}]
   (flet [response (client/post (format "https://sheets.googleapis.com/v4/spreadsheets/%s:batchUpdate", spreadsheet-id)
                                {:oauth-token auth/access-token
                                 :form-params (merge {:requests requests}
                                                     (when includeSpreadsheetInResponse
                                                       {:includeSpreadsheetInResponse includeSpreadsheetInResponse})
                                                     (when responseRanges
                                                       {:responseRanges responseRanges})
                                                     (when responseIncludeGridData
                                                       {:responseIncludeGridData responseIncludeGridData}))
                                 :content-type :json
                                 :as :json})]
     (:body response))))

(defn create-sheet
  [spreadsheet-id sheet]
  (f-> (batch-update spreadsheet-id [{:addSheet {:properties sheet}}])
       :replies
       first
       (get-in [:addSheet :properties])))

(defn append-row
  [spreadsheet-id sheet-id cells]
  (batch-update spreadsheet-id [{:appendCells
                                 {:sheetId sheet-id
                                  :rows [{:values [ (map (fn [x] {:userEnteredValue {:stringValue x}}) cells)]}]
                                  :fields "*"}}]))
