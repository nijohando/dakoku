(ns jp.nijohando.dakoku.google-sheets-writer.auth
  (:require
    [jp.nijohando.dakoku.google-sheets-writer.conf :as conf]
    [jp.nijohando.failable :refer [flet fail guard]]
    [jp.nijohando.deferable :refer [do* defer]]
    [clojure.tools.logging :as log]
    [clojure.java.io :as jio]
    [clojure.string :as s]
    [cheshire.core :as json]
    [buddy.core.keys :as keys]
    [buddy.sign.jwt :as jwt]
    [mount.core :refer [defstate]]
    [environ.core :refer [env]]
    [clj-http.client :as client])
  (:import
    (java.net URI)
    (java.io InputStreamReader
             BufferedReader)
    (com.amazonaws.services.s3 AmazonS3
                               AmazonS3Client
                               AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model GetObjectRequest)))


(defmulti ^:private load-private-key-json (fn [uri] (-> (.getScheme uri)
                                                        (keyword))))
(defmethod load-private-key-json :file [uri]
  (do*
    (flet [s (jio/reader uri)
           _ (defer (.close s))]
      (json/parse-stream (jio/reader uri) true))))

(defmethod load-private-key-json :s3 [uri]
  (do*
    (flet [bucket-name (.getAuthority uri)
           path (.getPath uri)
           object-key (if (s/starts-with? path "/")
                        (subs path 1)
                        path)
           request (GetObjectRequest. bucket-name object-key)
           s3client (AmazonS3ClientBuilder/defaultClient)
           s3object (.getObject s3client request)
           in (.getObjectContent s3object)
           rdr (jio/reader in)
           _ (defer (.close rdr))]
      (json/parse-stream rdr true))))

(defn- generate-json-web-token
  [private-key-json scopes delegated-user-email {:keys [expiration issue] :as times}]
  (flet [claim-set {:iss (:client_email private-key-json)
                    :scope (s/join " " scopes)
                    :aud "https://www.googleapis.com/oauth2/v4/token"
                    :sub delegated-user-email
                    :exp expiration
                    :iat issue}
         private-key (-> (:private_key private-key-json)
                         (keys/str->private-key))]
    (jwt/sign claim-set private-key {:alg :rs256})))

(defn- request-access-token
  [json-web-token]
  (client/post
    "https://www.googleapis.com/oauth2/v4/token"
    {:form-params {:grant_type "urn:ietf:params:oauth:grant-type:jwt-bearer"
                   :assertion json-web-token}
     :as :json}))

(defn acquire-access-token
  []
  (flet [now (quot (System/currentTimeMillis) 1000)
         private-key-json-url (conf/private-key-json-url)
         delegated-user-email (conf/delegated-user-email)
         private-key-json (load-private-key-json (URI/create private-key-json-url))
         json-web-token (generate-json-web-token
                          private-key-json
                          ["https://www.googleapis.com/auth/spreadsheets"
                           "https://www.googleapis.com/auth/drive"]
                          delegated-user-email
                          {:issue now
                           :expiration (+ now (* 60 10))})
         response (request-access-token json-web-token)]
    (or (get-in response [:body :access_token])
        (fail "Response does not contain an access token."))))

(defstate access-token :start (-> (acquire-access-token)
                                  (guard "Failed to acquire access token.")))
