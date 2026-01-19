(ns play.perception
  "Game state perception and overview functions for SOS settlement management.
   Provides quick summaries of population, resources, and settlement status."
  (:require
   [game.warehouse :as warehouse]
   [repl.utils :as utils]))

(require '[game.settlement :as settlement]
         '[game.humanoid :as humanoid]
         '[game.warehouse :as warehouse]
         '[game.building :as building]
         '[game.animal :as animal]
         '[game.common :refer [array-list->vec array-list-resize->vec]])
(import '[init.resources RESOURCES]
        '[settlement.main SETT]
        '[settlement.stats STATS]
        '[game.faction FACTIONS]
        '[init.type HCLASSES]
        '[init.race RACES]
        '[init.type NEEDS])

(defn settlement-overview
  "Get a comprehensive overview of the settlement status.
   Returns a map with population, resources, buildings, animals, and people status."
  []
  (let [entities (settlement/all-entities)
        ;; humanoid-info returns nil for non-humanoids, so filter those out
        entity-infos (filter identity (map humanoid/humanoid-info entities))

        ;; Count people by status
        count-status #(count (filter true? (map % entity-infos)))]

    {:population {:total (count entity-infos)
                  :adults (count (filter :isAdult entity-infos))
                  :cold (count-status :isCold)
                  :in-danger (count-status :inDanger)
                  :critical (count-status :critical)
                  :will-die (count-status :willDie)}

     :resources {:wood (warehouse/total-amount-for-resource (RESOURCES/WOOD))
                 :stone (warehouse/total-amount-for-resource (RESOURCES/STONE))
                 :food {:bread (warehouse/total-amount-for-resource-key "BREAD")
                        :meat (warehouse/total-amount-for-resource-key "MEAT")
                        :vegetable (warehouse/total-amount-for-resource-key "VEGETABLE")
                        :fruit (warehouse/total-amount-for-resource-key "FRUIT")
                        :fish (warehouse/total-amount-for-resource-key "FISH")
                        :grain (warehouse/total-amount-for-resource-key "GRAIN")
                        :mushroom (warehouse/total-amount-for-resource-key "MUSHROOM")
                        :ration (warehouse/total-amount-for-resource-key "RATION")}}

     :materials {:coal (warehouse/total-amount-for-resource-key "COAL")
                 :ore (warehouse/total-amount-for-resource-key "ORE")
                 :metal (warehouse/total-amount-for-resource-key "METAL")
                 :leather (warehouse/total-amount-for-resource-key "LEATHER")
                 :cloth (warehouse/total-amount-for-resource-key "CLOTHES")}

     :drinks {:wine (warehouse/total-amount-for-resource-key "ALCO_WINE")
              :beer (warehouse/total-amount-for-resource-key "ALCO_BEER")}

     :buildings {:total (building/blueprint-imp-count)
                 :by-category (building/rooms-by-category)}

     :animals {:wild (count (animal/wild-animals))
               :domesticated (count (animal/domesticated-animals))}}))

(defn print-settlement-overview
  "Print a human-readable overview of the settlement status."
  []
  (let [overview (settlement-overview)]
    (println "=== SETTLEMENT OVERVIEW ===")
    (println)

    ;; Population
    (println "Population:")
    (println "  Total:" (:total (:population overview)))
    (println "  Adults:" (:adults (:population overview)))
    (when (pos? (:cold (:population overview)))
      (println "  Cold:" (:cold (:population overview))))
    (when (pos? (:in-danger (:population overview)))
      (println "  In Danger:" (:in-danger (:population overview))))
    (when (pos? (:critical (:population overview)))
      (println "  Critical:" (:critical (:population overview))))
    (println)

    ;; Food Resources
    (println "Food Resources:")
    (let [food (:food (:resources overview))
          total-food (reduce + 0 (vals food))]
      (if (zero? total-food)
        (println "  NO FOOD - SETTLEMENT WILL STARVE!")
        (doseq [[food-name amount] food]
          (when (pos? amount)
            (println "  " (name food-name) ":" amount))))
      (println "  Total Food:" (reduce + 0 (vals food))))
    (println)

    ;; Basic Resources
    (println "Basic Resources:")
    (println "  Wood:" (:wood (:resources overview)))
    (println "  Stone:" (:stone (:resources overview)))
    (println)

    ;; Materials
    (println "Materials:")
    (println "  Coal:" (:coal (:materials overview)))
    (println "  Ore:" (:ore (:materials overview)))
    (println "  Metal:" (:metal (:materials overview)))
    (println "  Leather:" (:leather (:materials overview)))
    (println "  Clothes:" (:cloth (:materials overview)))
    (println)

    ;; Drinks
    (println "Drinks:")
    (println "  Wine:" (:wine (:drinks overview)))
    (println "  Beer:" (:beer (:drinks overview)))
    (println)

    ;; Buildings
    (println "Buildings:" (:total (:buildings overview)))
    (println)

    ;; Animals
    (println "Animals:")
    (println "  Wild:" (:wild (:animals overview)))
    (println "  Domesticated:" (:domesticated (:animals overview)))

    overview))

(comment
  (print-settlement-overview)
  :rcf)

(defn quick-status
  "Get a quick one-line status summary of critical issues."
  []
  (let [overview (settlement-overview)
        pop (:total (:population overview))
        cold (:cold (:population overview))
        danger (:in-danger (:population overview))
        food-total (reduce + 0 (vals (:food (:resources overview))))]

    (str "Pop:" pop
         (when (pos? cold) (str " | " cold " cold"))
         (when (pos? danger) (str " | " danger " danger"))
         (when (zero? food-total) " | NO FOOD!")
         (when (pos? food-total) (str " | " food-total " food")))))

(comment
  (quick-status)
  :rcf)

;; ============================================================================
;; Food Panel Data - UI Food Panel Equivalent
;; ============================================================================

(defn all-edible-resources
  "Get all edible resources."
  []
  (-> (RESOURCES/EDI)
      (.all)
      array-list->vec))

(defn food-production-rate
  "Get total food production rate per day (sum of all edible resources)."
  []
  (let [rooms (SETT/ROOMS)
        prod (.-PROD rooms)]
    (reduce + 0.0
            (map (fn [res-g]
                   (.produced prod (.resource res-g)))
                 (all-edible-resources)))))

(comment
  (food-production-rate)
  :rcf)

(defn food-consumption-rate
  "Get total food consumption rate per day.
   Includes: production consumption (e.g. mills), maintenance, and population hunger."
  []
  (let [rooms (SETT/ROOMS)
        prod (.-PROD rooms)
        maintenance (SETT/MAINTENANCE)]
    (reduce + 0.0
            (concat
             ;; Production consumption (e.g., mills consuming grain)
             (map (fn [res-g]
                    (+ (.consumed prod (.resource res-g))
                       (.estimateGlobal maintenance (.resource res-g))))
                  (all-edible-resources))
             ;; Population hunger consumption
             (for [hclass (array-list->vec (HCLASSES/ALL))
                   :when (.player hclass)
                   race (array-list->vec (RACES/all))]
               (let [hunger-rate (-> (NEEDS/TYPES)
                                    .-HUNGER
                                    .-rate
                                    (.get (.get hclass race)))
                     pop-count (-> (STATS/POP)
                                  .-POP
                                  (.data hclass)
                                  (.get race 0))
                     decree (-> (STATS/FOOD)
                              .-FOOD
                              (.decree)
                              (.get hclass race))]
                 (* hunger-rate pop-count decree)))))))

(comment
  (food-consumption-rate)
  :rcf)

(defn food-storage
  "Get total food storage across stockpile, eateries, and canteens."
  []
  (let [rooms (SETT/ROOMS)
        stockpile (.-STOCKPILE rooms)
        tally (.tally stockpile)]
    (reduce + 0
            (concat
             ;; Stockpile
             (map (fn [res-g]
                    (.amountTotal tally (.resource res-g)))
                  (all-edible-resources))
             ;; Eateries
             (map (fn [eatery]
                    (.totalFood eatery))
                  (array-list->vec (.-EATERIES rooms)))
             ;; Canteens
             (map (fn [canteen]
                    (.totalFood canteen))
                  (array-list->vec (.-CANTEENS rooms)))))))

(comment
  (food-storage)
  :rcf)

(defn food-days-remaining
  "Get the number of days of food remaining.
   Returns the actual days remaining (multiplied by dataDivider)."
  []
  (let [food-stats (STATS/FOOD)
        food-days (.-FOOD_DAYS food-stats)
        food-days-data (.data food-days)]
    (* (.getD food-days-data nil)
       (.dataDivider food-days))))

(comment
  (food-days-remaining)
  :rcf)

(defn food-status
  "Get comprehensive food status equivalent to UI food panel.
   Returns a map with production, consumption, storage, and days remaining."
  []
  {:production (food-production-rate)
   :consumption (food-consumption-rate)
   :storage (food-storage)
   :days-remaining (food-days-remaining)
   :net-rate (- (food-production-rate) (food-consumption-rate))})

(comment
  (food-status)
  :rcf)

(defn food-status-by-resource
  "Get food status breakdown by resource.
   Returns a map of resource name -> production, consumption, storage."
  []
  (let [rooms (SETT/ROOMS)
        prod (.-PROD rooms)
        stockpile (.-STOCKPILE rooms)
        tally (.tally stockpile)]
    (into {}
          (map (fn [res-g]
                 (let [resource (.resource res-g)
                       name (.toString (.name resource))]
                   [name
                    {:production (.produced prod resource)
                     :consumption (+ (.consumed prod resource)
                                    (.estimateGlobal (SETT/MAINTENANCE) resource))
                     :storage (.amountTotal tally resource)
                     :net (- (.produced prod resource)
                             (+ (.consumed prod resource)
                                (.estimateGlobal (SETT/MAINTENANCE) resource)))}]))
               (all-edible-resources)))))

(comment
  (food-status-by-resource)
  :rcf)

(defn print-food-status
  "Print a human-readable food status equivalent to UI food panel."
  []
  (let [status (food-status)
        by-resource (food-status-by-resource)]
    (println "=== FOOD STATUS ===")
    (println)
    (println "Production Rate:" (format "%.2f" (:production status)) "/day")
    (println "Consumption Rate:" (format "%.2f" (:consumption status)) "/day")
    (println "Net Rate:" (format "%.2f" (:net-rate status)) "/day")
    (println)
    (println "Total Storage:" (:storage status))
    (println "Days Remaining:" (format "%.1f" (:days-remaining status)))
    (println)
    (println "By Resource:")
    (doseq [[name info] (sort-by (fn [[n _]] n) by-resource)]
      (println "  " name ":")
      (println "    Production:" (format "%.2f" (:production info)))
      (println "    Consumption:" (format "%.2f" (:consumption info)))
      (println "    Net:" (format "%.2f" (:net info)))
      (println "    Storage:" (:storage info)))
    status))

(comment
  (print-food-status)
  :rcf)

(defn food-quick-status
  "Get a quick one-line food status summary."
  []
  (let [status (food-status)
        days (:days-remaining status)]
    (str "Food: " (:storage status)
         " (" (format "%.1f" days) " days"
         (when (pos? (:net-rate status))
           (str ", +" (format "%.2f" (:net-rate status)) "/day"))
         (when (neg? (:net-rate status))
           (str ", " (format "%.2f" (:net-rate status)) "/day"))
         ")")))

(comment
  (food-quick-status)
  :rcf)
