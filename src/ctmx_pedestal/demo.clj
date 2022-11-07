(ns ctmx-pedestal.demo
  (:require
   [ctmx-pedestal.mapped-forms :as mapped.forms]
   [ctmx.core :as ctmx :refer [defcomponent make-routes]]
   [hiccup.page :as hiccup.page]
   [reitit.http.interceptors.parameters :as parameters]
   [ring.middleware.keyword-params :as keyword-params]
   [io.pedestal.http :as server]
   [reitit.pedestal :as pedestal]
   [reitit.http :as http])
  (:refer-clojure :exclude [parse-long]))

(defn page [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body
   (hiccup.page/html5
    [:body body
     [:script {:src "https://unpkg.com/htmx.org@1.2.0"}]
     [:script "htmx.config.defaultSwapStyle = 'outerHTML';"]
     [:script "htmx.config.defaultSettleDelay = 0;"]])})

;;; This interceptor is required otherwise CTMX cannot parse the parameters
(def keyword-params-interceptor
  {:name ::keyword-params
   :enter (fn [ctx]
            (let [request (:request ctx)]
              (assoc ctx :request
                     (keyword-params/keyword-params-request request))))})

(def default-interceptors [;; query-params & form-params
                           (parameters/parameters-interceptor)
                           ;; htmx requires all params (query, form etc) to be keywordized
                           ;; (To test the breakage, comment out this interceptor, reval and restart)
                           keyword-params-interceptor
                           ;;
                           ])

(defn parse-long [v]
  (if (int? v)
    v
    (clojure.core/parse-long v)))

(defcomponent ^:endpoint demo-comp [req val]
  (let [current (or (value "val") val 0)
        next-val  (inc (parse-long current))]
    [:form {:id id :hx-post "demo-comp"}
     [:h2 "Current Value: " current]
     [:input {:type "hidden" :name (path "val") :value next-val}]
     [:button {:type :submit :hx-target (hash ".")} "Increment"]]))

(def routes
  ["" {:interceptors default-interceptors}
   ["/"
    {:get {:handler (fn [req]
                      {:status 200 :body "Go to /ctmx-demo"})}}]
   (make-routes "/mapped-forms"
                (fn [req]
                  (page (mapped.forms/mapped-forms req))))

   (make-routes "/ctmx-demo"
                (fn [req]
                  (page (demo-comp req 0))))])

(defn build-service-map []
  (-> {::server/type :jetty
       ::server/port 3000
       ::server/join? false
       ;; disable CSP for this demo
       ::server/secure-headers {:content-security-policy-settings {}}
       ;; no pedestal routes
       ::server/routes []}
      (server/default-interceptors)
      ;; swap the reitit router
      (pedestal/replace-last-interceptor
       (pedestal/routing-interceptor
        (http/router (doto routes tap>))))
      (server/dev-interceptors)))

(defn start-server []
  (->
   (build-service-map)
   (server/create-server)
   (server/start)))

(defonce server (atom nil))

(defn stop-server []
  (server/stop @server)
  (reset! server nil))

(defn start []
  (if-not @server
    (reset! server (start-server))
    "Server already running."))

(defn stop []
  (when @server
    (stop-server)))

(comment

  ;; Start/Stop
  (do
    (stop)
    (start)) ;; rcf

  ;;  open localhost:3000/ctmx-demo
  )
