(ns api
  (:gen-class)
  (:require 
   [integrant.core :as ig] 
   [replicant.logging :as logging] 
   [taoensso.telemere :as tel] 
   [ws-server :as ws-server]
   [clojure.edn :as edn]
   [reitit.ring :as ring]
   [ring.util.response :as response]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.params :as params]
   [ring.middleware.file :refer [wrap-file]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.cors :refer [wrap-cors]]
   [clojure.core.async :as async :refer [go-loop go <! <!!]]
   [muuntaja.middleware :as middleware]))

(defn html-handler [_request]
  (response/redirect "index.html"))

(defn query-handler [query]
  (or
      #_(streaming-playground-api/query-handler query)))

(defn http-query-handler [request]
  (let [query (:body-params request)
        result (query-handler query)]
    (response/response {:success? true
                        :result result})))

(defn ws-query-handler [{:keys [event uid client-id ?data id ?reply-fn]}]
  (let [query ?data
        result (query-handler query)]
    (println "result" query result)
    (when ?reply-fn
      (?reply-fn result))))

(comment
  (ws-query-handler {:?data {:query/kind :query/script-config}})
  (ws-query-handler {:?data {:query/kind :query/browser-devices
                             :query/data {:page 1, :size 20, :filter {}}}})
  :rcf)

(defn command-handler [command]
  (or #_(streaming-playground-api/command-handler command)))

(defn http-command-handler [request]
  (let [command (:body-params request)
        result (command-handler command)]
    (response/response {:success? true
                        :result result})))

(defn ws-command-handler [{:keys [event uid client-id ?data id ?reply-fn]}]
  (let [command ?data
        result (command-handler command)]
    (when ?reply-fn
      (?reply-fn result))))

(defn upload-handler [request]
  (let [file (get-in request [:multipart-params "file"])
        payload (edn/read-string (get-in request [:multipart-params "payload"]))
        {:command/keys [kind data]} payload
        content (slurp (:tempfile file))]
    (case kind
      #_#_:command/upload-session
      (do
        (backend/add-session! backend/conn content)
        (response/response {:success? true})))))


(defn my-wrap-cors [handler]
  (wrap-cors handler :access-control-allow-origin [#"http://localhost:8000"]
             :access-control-allow-methods [:get :put :post :delete]))

(defn make-routes [{:keys [ring-ajax-get-or-ws-handshake ring-ajax-post] :as ws-server}]
  [["/" {:get html-handler
         :post html-handler}]
   ["/chsk" {:middleware [params/wrap-params
                          wrap-keyword-params
                          wrap-session]
             :get ring-ajax-get-or-ws-handshake
             :post ring-ajax-post}]
   ["/api" {:middleware [params/wrap-params
                         middleware/wrap-format
                         my-wrap-cors]} 
    ["/query" {:post http-query-handler}]
    ["/command" {:post http-command-handler}]
    ["/upload" {:middleware [wrap-multipart-params]
                :post upload-handler}]]])

(defn ws-handler [stop-ch {:keys [ch-chsk] :as ws-server}]
  (go-loop []
    (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
      (when (= port ch-chsk)
        (let [{:keys [event uid client-id ?data id ?reply-fn]} event-msg] 
          (try 
            (case id
              :chsk/ws-ping nil 
              :test/echo (?reply-fn [id ?data])
              :data/query (ws-query-handler event-msg)
              :data/command (ws-command-handler event-msg)
              nil)
            (catch Exception e
              (tel/error! e))))
        (recur)))))

(defn make-handler [routes]
  (wrap-file 
   (ring/ring-handler
    (ring/router routes))
   "public"))

(def config
  {:jetty/routes (ig/ref :adapter/ws-server)
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}
   :adapter/ws-server true
   :adapter/ws-handler {:ws-server (ig/ref :adapter/ws-server)}
   :log/logging true})

(defmethod ig/init-key :jetty/routes [_ ws-server]
  (make-routes ws-server))

(defmethod ig/init-key :jetty/handler [_ routes]
  (make-handler routes))

(defmethod ig/init-key :adapter/jetty [_ {:keys [port handler]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defmethod ig/init-key :adapter/ws-server [_ _]
  (ws-server/make-ws-server))

(defmethod ig/init-key :adapter/ws-handler [_ {:keys [ws-server]}]
  (let [stop-chan (async/chan)]
    (ws-handler stop-chan ws-server)
    stop-chan))

(defmethod ig/halt-key! :adapter/ws-handler [_ stop-chan]
  (async/put! stop-chan :stop))

(defmethod ig/init-key :log/logging [_ _]
  (logging/init))

(defn -main [& _]
  (ig/init config))

(comment 
  ;; 启动这个
  (def system (-main))
  :rcf)

;; web socket 相关测试
(comment
  (def chsk-send! (:chsk-send! (:adapter/ws-server system)))
  (def connected-uids (:connected-uids (:adapter/ws-server system)))

  (chsk-send!
   "uid" ;; user id 
   [:subscribe/event 1] ; Event
   8000 ; Timeout
   )
  :rcf)



