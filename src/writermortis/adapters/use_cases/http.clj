(ns writermortis.adapters.use-cases.http
  (:require [io.pedestal.http :as http]
            [writermortis.adapters.repository.user-record :as user-record]
            [writermortis.core.auth :as auth]
            [writermortis.adapters.mappers.user-mapper :as user-mapper]))

(defn health
  [request]
  (http/json-response {:health "ok"
                       :clojure-version (clojure-version)}))

(def fetch-user-for-auth
  {:name :fetch-user-for-auth
   :enter
   (fn [context]
     (assoc context :authenticating-user
            (user-record/find-by-username
             (get-in context [:request :json-params :username]))))})

(def validate-password
  {:name :validate-password
   :enter
   (fn [context]
     (let
      [password         (get-in context [:request :json-params :password])
       user-record      (:authenticating-user context)
       did-authenticate (auth/verify-password  user-record password)]
       (if did-authenticate
         (assoc context :current-user (:authenticating-user context))
         ;; TODO: json stringify response
         (assoc context :response {:status 401 :body "Not Authorized"}))))})

(def access-token
  {:name :access-token
   :enter
   (fn [context]
     (assoc context :access-token (get-in context [:current-user :id])))})

(def set-access-token-cookie
  {:name :set-access-token-cookie
   :leave
   (fn [context]
     (if (:access-token context)
       (assoc-in context [:response :cookies]
                 {"access-token" {:value (:access-token context)}})
       (assoc-in context [:response :cookies]
                 (get-in context [:request :cookies]))))})

(def login-render
  {:name :login-render
   :leave
   (fn [context]
     (let
      [json-response
       (http/json-response (user-mapper/map-record (:current-user context)))]
         ;; TODO: There's gotta be a better way to do this...
       (assoc context :response (merge json-response (:response context)))))})

;; debugger

(def context-to-body
  {:name :context-to-body
   :leave
   (fn [context]
     (assoc context :response {:status 200 :body context}))})