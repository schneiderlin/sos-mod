(ns replicant.main
  (:require
   [integrant.core :as ig]
   [replicant.dom :as r]
   [replicant.alias :as alias]
   [replicant.utils :refer [interpolate]]
   [replicant.ws-client :as ws-client]
   [dataspex.core :as dataspex]
   [clojure.core.async :as async]
   [replicant.timer :as timer]
   [replicant.render :as render]
   [replicant.actions :as actions]
   [replicant.router :as router]))

(defn routing-anchor [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href (router/location->url routes
                                                   (:ui/location attrs))))]
          children)))

(alias/register! :ui/a routing-anchor)

(defn get-current-location []
  (->> js/location.href
       (router/url->location router/routes)))

(def config
  {:replicant/el "app"
   :replicant/store nil
   :replicant/dataspex (ig/ref :replicant/store)
   :replicant/render-loop {:store (ig/ref :replicant/store)
                           :el (ig/ref :replicant/el)
                           #_#_:ws-client (ig/ref :adapter/ws-client)}
   #_#_:adapter/ws-client true
   #_#_:adapter/ws-handler {:ws-client (ig/ref :adapter/ws-client)}})

(defmethod ig/init-key :adapter/ws-client [_ _]
  (ws-client/make-ws-client))

(defmethod ig/init-key :adapter/ws-handler [_ {:keys [ws-client]}]
  (let [stop-ch (async/chan)]
    (ws-client/ws-handler stop-ch ws-client)
    stop-ch))

(defmethod ig/halt-key! :adapter/ws-handler [_ stop-ch]
  (async/put! stop-ch :stop))

(defmethod ig/init-key :replicant/el [_ el]
  (js/document.getElementById el))

(defmethod ig/init-key :replicant/store [_ init-value]
  (atom init-value))

(defmethod ig/init-key :replicant/dataspex [_ store]
  (dataspex/inspect "store" store))

(defmethod ig/init-key :replicant/render-loop [_ {:keys [el store] :as system}]

  (add-watch
   store ::render
   (fn [_ _ _ state]
     (r/render el (render/render-page state) {:alias-data {:routes router/routes}})))

  (add-watch
   timer/!timers :timers
   (fn [_ _ old-timers new-timers]
     (when (not= (keys old-timers) (keys new-timers))
       (timer/start-timer store))))

  (r/set-dispatch!
   (fn [{:keys [replicant/dom-event]} actions]
     (->> actions
          (interpolate dom-event)
          (actions/execute-actions system store dom-event))))

  (js/document.body.addEventListener
   "click"
   #(router/route-click % system store router/routes actions/execute-actions))

  (swap! store assoc
         :app/started-at (js/Date.)
         :dispatch actions/execute-actions
         :location (get-current-location)))

(defn ^:dev/after-load main []
  (ig/init config))


(comment 
  ;; TODO 用了 integrant 之后拿不到 store
  :rcf)

