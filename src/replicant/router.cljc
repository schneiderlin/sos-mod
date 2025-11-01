(ns replicant.router
  (:require
   [domkm.silk :as silk] 
   [lambdaisland.uri :as uri]))

(def routes
  (silk/routes
   (concat
    ;; some router
    #_test2.router/routes
    [[:pages/frontpage [["home"]]]])))

(defn get-location-load-actions [location]
  (or 
      #_(test2.router/get-location-load-actions location)))

(defn url->location [routes url]
  (let [uri (cond-> url (string? url) uri/uri)]
    (when-let [arrived (silk/arrive routes (:path uri))]
      (let [query-params (uri/query-map uri)
            hash-params (some-> uri :fragment uri/query-string->map)]
        (cond-> {:location/page-id (:domkm.silk/name arrived)
                 :location/params (dissoc arrived
                                          :domkm.silk/name
                                          :domkm.silk/pattern
                                          :domkm.silk/routes
                                          :domkm.silk/url)}
          (seq query-params) (assoc :location/query-params query-params)
          (seq hash-params) (assoc :location/hash-params hash-params))))))

(defn location->url [routes {:location/keys [page-id params query-params hash-params]}]
  (cond-> (silk/depart routes page-id params)
    (seq query-params)
    (str "?" (uri/map->query-string query-params))

    (seq hash-params)
    (str "#" (uri/map->query-string hash-params))))

(defn essentially-same? [l1 l2]
  (and (= (:location/page-id l1) (:location/page-id l2))
       (= (not-empty (:location/params l1))
          (not-empty (:location/params l2)))
       (= (not-empty (:location/query-params l1))
          (not-empty (:location/query-params l2)))))

(defn find-target-href [e]
  (some-> e .-target
          (.closest "a")
          (.getAttribute "href")))

(defn navigate! [system store new-location execute-actions]
  (let [current-location (:location @store)
        url (location->url routes new-location)
        load-actions (get-location-load-actions new-location)]
    (swap! store assoc :location new-location)
    (when (not (essentially-same? current-location new-location))
      #?(:cljs (.pushState js/history nil "" url))
      (println "load-actions" load-actions)
      (execute-actions system store nil load-actions))))

(defn route-click [e system store routes execute-actions]
  (let [href (find-target-href e)]
    (when-let [location (url->location routes href)]
      (.preventDefault e)
      (navigate! system store location execute-actions))))
