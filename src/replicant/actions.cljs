(ns replicant.actions
  (:require
   [ui.multi-input :as multi-input]
   [ui.multi-select :as multi-select]
   [component.table :as table]
   [component.server-select-filter :as server-filter]
   [component.completion-input :as completion-input] 
   [clojure.edn :as reader]
   [replicant.utils :refer [interpolate]]
   [replicant.ws-client :as ws-client]
   [replicant.query :as query]
   [replicant.command :as command]
   [replicant.router :refer [navigate!]]))

(declare execute-actions)

(defn dispatch-actions [system store actions]
  (execute-actions system store nil actions))

(defn query-backend [system store query & [{:keys [on-success]}]]
  (swap! store query/send-request (js/Date.) query)
  (-> (js/fetch "/api/query"
                #js {:method "POST"
                     :body (pr-str query)
                     :headers #js {"accept" "application/edn"
                                   "content-type" "application/edn"}})
      (.then #(.text %))
      (.then reader/read-string)
      (.then (fn [{:keys [result] :as res}]
               (swap! store query/receive-response (js/Date.) query res)
               (when on-success
                 (let [actions (if (fn? on-success)
                                 (on-success result)
                                 (interpolate result on-success))]
                   (execute-actions system store nil actions)))))
      (.catch #(swap! store query/receive-response
                      (js/Date.)
                      query {:error (.-message %)}))))

(defn issue-command [system store command & [{:keys [on-success]}]]
  (swap! store command/issue-command (js/Date.) command)
  (-> (js/fetch "/api/command"
                #js {:method "POST"
                     :body (pr-str command)
                     :headers #js {"accept" "application/edn"
                                   "content-type" "application/edn"}})
      (.then #(.text %))
      (.then reader/read-string)
      (.then (fn [res]
               (swap! store command/receive-response
                      (js/Date.) command res)
               (when on-success
                 (execute-actions system store nil on-success))))
      (.catch #(swap! store command/receive-response
                      (js/Date.) command {:error (.-message %)}))))

(defn choose-file [system store choose-args & [{:keys [on-success]}]]
  (let [input (js/document.createElement "input")]
    (set! (.-type input) "file")
    (set! (.-onchange input)
          (fn [e]
            (let [file (-> e .-target .-files (aget 0))]
              (when file
                (when on-success
                  (->> on-success
                       (interpolate e)
                       (execute-actions system store nil)))))))
    (.click input)))

(defn upload-file [system store command & [{:keys [on-success]}]]
  (let [file (:command/file command)
        form-data (js/FormData.)]
    (.append form-data "file" file)
    (.append form-data "payload" (pr-str (dissoc command :command/file)))
    (swap! store command/issue-command (js/Date.) command)
    (-> (js/fetch "/api/upload"
                  #js {:method "POST"
                       :body form-data})
        (.then #(.text %))
        (.then reader/read-string)
        (.then (fn [res]
                 (swap! store command/receive-response
                        (js/Date.) command res)
                 (when on-success
                   (execute-actions system store nil on-success))))
        (.catch #(swap! store command/receive-response
                        (js/Date.) command {:error (.-message %)})))))

(defn execute-actions [system store e actions]
  (doseq [[action & args] actions]
    (let [result (or (case action
                       :store/assoc (apply swap! store assoc args)
                       :store/assoc-in (apply swap! store assoc-in args)
                       :store/update-in (apply swap! store update-in args)
                       :event/prevent-default (.preventDefault e)
                       :event/stop-propagation  (.stopPropagation e)
                       :event/clear-input (set! (.-value (.-target e)) "")
                       :key/press (let [[k actions] args] 
                                    (when (= (.-key e) k)
                                      (execute-actions system store e actions)))
                       :router/navigate (navigate! system store (first args) execute-actions)
                       :data/query (apply query-backend system store args)
                       :data/command (apply issue-command system store args)
                       :data/choose-file (apply choose-file system store args)
                       :data/upload (apply upload-file system store args)
                       :debug/print (js/console.log (clj->js args))
                       :clipboard/copy (js/navigator.clipboard.writeText (first args))
                       ;;  :timer/register (apply timer/register-timer args)
                       nil)
                     ;; lib extensions
                     (table/execute-action store e action args)
                     (multi-input/execute-action store e action args)
                     (multi-select/execute-action store e action args)
                     (server-filter/execute-action store e action args)
                     (completion-input/execute-action store e action args)
                     ;; app extensions 
                     #_(streaming-playground-actions/execute-action store e action args))]
      (when (vector? result)
        (execute-actions system store e result)))))
