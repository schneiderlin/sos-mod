(ns draft.d3)

(defonce el (js/document.getElementById "graph"))
(defonce stats (js/document.getElementById "stats"))

(defn dragstarted [^js event ^js simulation]
              (when (not (.-active event))
                (-> (.alphaTarget simulation 0.3)
                    (.restart)))
              (set! (.. event -subject -fx) (.. event -subject -x))
              (set! (.. event -subject -fy) (.. event -subject -y)))

(defn dragged [^js event]
          (set! (.. event -subject -fx) (.. event -x))
          (set! (.. event -subject -fy) (.. event -y)))

(defn dragended [^js event ^js simulation]
            (when (not (.-active event))
              (.alphaTarget simulation 0))
            (set! (.. event -subject -fx) nil)
            (set! (.. event -subject -fy) nil))

(defn drag [^js simulation]
  (-> (.drag js/d3)
      (.on "start" (fn [^js event] (dragstarted event simulation)))
      (.on "drag" (fn [^js event] (dragged event)))
      (.on "end" (fn [^js event] (dragended event simulation)))))

(defn init-visualization [^js graph]
  (let [svg (js/d3.select "#graph")
        width js/window.innerWidth
        height js/window.innerHeight]

    ;; update stats
    (set! (.-innerHTML stats)
          (str "Nodes: " (count (.-nodes graph)) "<br>Links: " (count (.-links graph))))

    ;; Clear previous content
    (.remove (.selectAll svg "*"))

    ;; Create force simulation
    (let [simulation (doto (.forceSimulation js/d3 (.-nodes graph))
                       (.force "link" (-> (.forceLink js/d3 (.-links graph))
                                          (.id (fn [d] (.-id d)))
                                          (.distance 100)))
                       (.force "charge" (-> (.forceManyBody js/d3)
                                            (.strength -300)))
                       (.force "center" (.forceCenter js/d3 (/ width 2) (/ height 2)))
                       (.force "collision" (-> (.forceCollide js/d3)
                                               (.radius 20))))
          link (-> (.append svg "g")
                   (.attr "class" "links")
                   (.selectAll "line")
                   (.data (.-links graph))
                   (-> (.enter) (.append "line")
                       (.attr "class" "link")
                       (.attr "stroke-width" (fn [d] (js/Math.sqrt 1)))))
          node (-> (.append svg "g")
                   (.attr "class" "nodes")
                   (.selectAll "g")
                   (.data (.-nodes graph))
                   (-> (.enter) (.append "g")
                       (.attr "class" "node")
                       #_(.call (drag simulation))))

          _labels (-> (.append node "text")
                      (.text (fn [d] (.-id d)))
                      (.style "font-size" "10px")
                      (.style "opacity" 1))

          zoom (-> (.zoom js/d3)
                   (.scaleExtent #js [0.1 4])
                   (.on "zoom" (fn [event]
                                 (-> (.select svg "g.nodes")
                                     (.attr "transform" (.-transform event)))
                                 (-> (.select svg "g.links")
                                     (.attr "transform" (.-transform event))))))]
      (-> (.append node "circle")
          (.attr "r" 8)
          (.on "mouseover"
               (fn [event d]
                 (this-as this
                          (-> (.select js/d3 this)
                              (.classed "selected" true)
                              (.attr "r" 12))
                          ;; highlight connected nodes and links
                          (.style link "stroke-opacity"
                                  (fn [l]
                                    (if (or (= (.-source l) d)
                                            (= (.-target l) d))
                                      1
                                      0.1)))
                          (-> (.select node "circle")
                              (.style "opacity"
                                      (fn [n]
                                        (if (or (= n d)
                                                (.some (.-links graph)
                                                       (fn [l]
                                                         (or (and (= (.-source l) d)
                                                                  (= (.-target l) n))
                                                             (and (= (.-source l) n)
                                                                  (= (.-target l) d))))))
                                          1
                                          0.1)))))))
          (.on "mouseout"
               (fn [event d]
                 (this-as this
                          (-> (.select js/d3 this)
                              (.classed "selected" false)
                              (.attr "r" 8))

                          ;; reset all links and nodes
                          (.style link "stroke-opacity" 0.6)
                          (-> (.select node "circle")
                              (.style "opacity" 1))))))

      (.on simulation "tick" (fn []
                               (-> link
                                   (.attr "x1" (fn [d] (.-x (.-source d))))
                                   (.attr "y1" (fn [d] (.-y (.-source d))))
                                   (.attr "x2" (fn [d] (.-x (.-target d))))
                                   (.attr "y2" (fn [d] (.-y (.-target d)))))

                               (-> node
                                   (.attr "transform" (fn [d] (str "translate(" (.-x d) "," (.-y d) ")"))))))

      (.call svg zoom))))

(defn ^:dev/after-load main []
  (let [graph (atom nil)
        _ (.then (js/d3.json "data/friend.json")
                 (fn [data] 
                   (init-visualization data)
                   (reset! graph data)))]
    ))
