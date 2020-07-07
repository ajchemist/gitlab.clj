(ns gitlab.v4.alpha
  (:require
   [clojure.java.io :as jio]
   [buddy.core.codecs :as codecs]
   [buddy.core.codecs.base64 :as base64]
   [clj-http.client :as http]
   [taoensso.timbre :as timbre]
   [user.ring.alpha :as user.ring]
   [user.uri.alpha :as user.uri]
   )
  (:import
   java.io.StringReader
   ))


(def ^:dynamic *origin* "https://gitlab.com")


(def ^{:arglists '([request] [request response raise])}
  request
  (-> http/request
    (user.ring/wrap-transform-request
      (fn [req]
        (merge
          {:as :json-string-keys}
          req)))
    (user.ring/wrap-transform-request
      (fn [{:keys [:gitlab/private-token] :as req}]
        (update-in req [:headers "Private-Token"]
          #(or % private-token))))
    (user.ring/wrap-meta-response)))


;;


(defn- format-request-url--repository-files
  ([project-id file-path]
   {:pre [(string? project-id)
          (string? file-path)]}
   (str *origin* "/api/v4/projects/" project-id "/repository/files/" (user.uri/encode-uri-component file-path)))
  ([project-id file-path tail]
   (str (format-request-url--repository-files project-id file-path) tail)))


(def ^{:arglists '([request] [request response raise])}
  request--repository-files-get
  (-> request
    (user.ring/wrap-transform-request
      (fn [{:keys [:gitlab/project-id :gitlab/file-path] :as req}]
        (-> req
          (update :url #(or % (format-request-url--repository-files project-id file-path)))
          (assoc :request-method :get)
          (update-in [:query-params :ref] #(or % "master")))))))


(def ^{:arglists '([request] [request response raise])}
  request--repository-files-get-blame
  (-> request
    (user.ring/wrap-transform-request
      (fn [{:keys [:gitlab/project-id :gitlab/file-path] :as req}]
        (-> req
          (update :url #(or % (format-request-url--repository-files project-id file-path "/blame")))
          (assoc :request-method :get)
          (update-in [:query-params :ref] #(or % "master")))))))


(def ^{:arglists '([request] [request response raise])}
  request--repository-files-get-raw
  (-> request
    ((fn
       [handler]
       (fn
         ([{:keys [url] :as req}]
          (try
            (handler req)
            (catch Exception e
              (timbre/error (ex-message e) (str "Request url: " url))
              (throw e))))
         ([req resp raise]
          (handler req resp raise)))))
    (user.ring/wrap-transform-request
      (fn [{:keys [:gitlab/project-id :gitlab/file-path] :as req}]
        (-> req
          (update :url #(or % (format-request-url--repository-files project-id file-path "/raw")))
          (assoc
            :request-method :get
            :as :byte-array)
          (update-in [:query-params :ref] #(or % "master")))))
    (user.ring/wrap-transform-response
      (fn [resp]
        (update resp :body (fn [body] (if (bytes? body) (codecs/bytes->str body) body)))))))


;; * Types


(defrecord RepositoryFile [private-token project-id file-path]
  jio/IOFactory
  (make-reader [x opts]
    (StringReader.
      (:body
       (request--repository-files-get-raw
         {:gitlab/private-token private-token
          :gitlab/project-id    project-id
          :gitlab/file-path     file-path})))))
