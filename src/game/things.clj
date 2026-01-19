(ns game.things
  "Ground items and scattered resources in SOS.
   Provides access to loose items on the ground (not in warehouses)."
  (:import [settlement.main SETT]
           [init.resources RESOURCES]))

(defn resources-at-tile
  "Get all scattered resources (ground items) at a specific tile.
   Returns a map with total counts by resource type."
  [tx ty]
  (let [things (SETT/THINGS)
        result (.get things tx ty)]
    (loop [i 0
           totals {}]
      (if (>= i (.size result))
        totals
        (let [item (.get result i)
              class-name (str (class item))]
          (if (.contains class-name "ScatteredResource")
            (let [resource-key (.key (.resource item))
                  amount (.amount item)]
              (recur (inc i) (update totals resource-key (fnil + 0) amount)))
            (recur (inc i) totals)))))))

(defn resources-in-area
  "Get all scattered resources within a rectangular area.
   Returns a map with total counts by resource type."
  [start-x start-y width height]
  (loop [x start-x
         totals {}]
    (if (>= x (+ start-x width))
      totals
      (recur (inc x)
             (loop [y start-y
                    totals totals]
               (if (>= y (+ start-y height))
                 totals
                 (recur (inc y) (merge-with + totals (resources-at-tile x y)))))))))

(comment
  (resources-at-tile 149 485)
  :rcf)
