(ns jp.nijohando.dakoku.google-sheets-writer.drive
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as s]
    [clj-http.client :as client]
    [jp.nijohando.dakoku.google-sheets-writer.auth :as auth]
    [jp.nijohando.failable :refer [flet fail]]))

(declare find-files)

(defn- gather-path-info
  [path-segments]
  (flet [root-is #(format "(name='%s' and 'root' in parents)", %)
         any (fn [field values]
               (->> (map #(format "name='%s'", %) values)
                    (s/join " or ")))
         q (str "(" (root-is (first path-segments))
             (when-let [xs (not-empty (rest path-segments))]
               (str " or (" (any "name" xs) ")"))
             ") and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
         result (find-files {:fields "files(id, name, parents)" :query q})]
    (->> result
         (map (fn [x] [(:name x) x]))
         (into {}))))

(defn- get-path-folders
  [path]
  (flet [path-segments (-> (if (s/starts-with? path "/")
                             (subs path 1)
                             path)
                           (s/split #"/"))
         path-info (gather-path-info path-segments)
         folders (map #(merge (get path-info %) {:name %}) path-segments)
         linked-list (partition 2 (interleave folders (concat [nil] folders)))
         f (fn [ctx [current parent]]
             (let [{:keys [id name parents]} current
                   parent-id (:id parent)]
               (if (nil? ctx)
                 (if id
                   [current]
                   (reduced folders))
                 (if (and id (some #(= parent-id %) parents))
                   (conj ctx current)
                   (conj ctx {:name name})))))]
    (reduce f nil linked-list)))

(defn find-files
  [{:keys [query fields] :as opt}]
  (flet [qp (merge {}
              (when query {:q query})
              (when fields {:fields fields}))
         response (client/get "https://www.googleapis.com/drive/v3/files"
                    {:query-params qp
                     :oauth-token auth/access-token
                     :as :json})]
    (get-in response [:body :files])))

(defn create-file
  [entity]
  (flet [response (client/post "https://www.googleapis.com/drive/v3/files"
                    {:form-params entity
                     :oauth-token auth/access-token
                     :content-type :json
                     :as :json})]
    (:body response)))

(defn create-folder
  [entity]
  (create-file (merge entity {:mimeType "application/vnd.google-apps.folder"})))

(defn get-folder
  [path]
  (flet [folders (get-path-folders path)]
    (loop [parent nil [node & others] folders]
      (let [pid (or (:id parent) "root")
            exists? #(not (nil? (:id %)))
            current (if (exists? node)
                      node
                      (create-folder {:name (:name node)
                                      :parents [pid]}))]
        (if (empty? others)
          current
          (recur current others))))))

