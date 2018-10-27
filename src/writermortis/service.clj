(ns writermortis.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [writermortis.adapters.repository.user-record :as user-record]
            [writermortis.adapters.mappers.user-mapper :as user-mapper]
            [writermortis.core.auth :as auth]
            [ring.util.response :as ring-resp]))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn home-page
  [request]
  (ring-resp/response "Hello World!"))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/html-body])

(def fetch-user-for-auth
  {:name :fetch-user-for-auth
   :enter
   (fn [context]
     (assoc context :authenticating-user (user-record/find-by-username (get-in context [:request :json-params :username]))))})

(def validate-password
  {:name :validate-password
   :enter
   (fn [context]
     (if-let [current-user (auth/verify-password (:authenticating-user context) (get-in context [:request :json-params :password]))]
       (assoc context :current-user (:authenticating-user context))
       ;; TODO: json response
       (assoc context :response {:status 401 :body "Not Authorized"})))})

(def set-access-token
  {:name :set-access-token
   :leave
   ;; TODO: Only if the user successfull auth'd
   (fn [context] (assoc-in context [:response :cookies] {"access-token" {:value "aaa.bbb.ccc"}}))})

(def login-render
  {:name :login-render
   :leave
   (fn [context]
     (let [json-response (http/json-response (user-mapper/map (:current-user context)))]
       ;; TODO: There's gotta be a better way to do this...
       (assoc context :response (merge json-response (:response context)))))})

(def return-context
  {:name :return-context
   :leave
   (fn [context]
     (assoc context :response {:status 200 :body context}))})

(def routes #{["/"        :get (conj common-interceptors `home-page)]
              ["/about"   :get (conj common-interceptors `about-page)]
              ["/login"   :post [login-render (body-params/body-params) middlewares/cookies set-access-token fetch-user-for-auth validate-password]]})

;; Consumed by writermortis.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ::http/type :jetty
              ;;::http/host "localhost"
              ::http/port 8085
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})

