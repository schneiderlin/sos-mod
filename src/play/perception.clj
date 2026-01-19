(ns play.perception
  "Game state perception and overview functions for SOS settlement management.
   Provides quick summaries of population, resources, and settlement status.")

(require '[game.humanoid :as humanoid])
(require '[game.warehouse :as warehouse])
(require '[game.building :as building])
(require '[game.animal :as animal])
(import '[init.resources RESOURCES])

(defn settlement-overview
  "Get a comprehensive overview of the settlement status.
   Returns a map with population, resources, buildings, animals, and people status."
  []
  (let [entities (humanoid/all-entities)
        entity-infos (map humanoid/humanoid-info entities)

        ;; Count people by status
        count-status #(count (filter true? (map % entity-infos)))]

    {:population {:total (count entities)
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
    (println "👥 Population:")
    (println "  Total:" (:total (:population overview)))
    (println "  Adults:" (:adults (:population overview)))
    (when (pos? (:cold (:population overview)))
      (println "  ⚠️  Cold:" (:cold (:population overview))))
    (when (pos? (:in-danger (:population overview)))
      (println "  🚨 In Danger:" (:in-danger (:population overview))))
    (when (pos? (:critical (:population overview)))
      (println "  💀 Critical:" (:critical (:population overview))))
    (println)

    ;; Food Resources
    (println "🍞 Food Resources:")
    (let [food (:food (:resources overview))
          total-food (reduce + 0 (vals food))]
      (if (zero? total-food)
        (println "  ⚠️  NO FOOD - SETTLEMENT WILL STARVE!")
        (doseq [[food-name amount] food]
          (when (pos? amount)
            (println "  " (name food-name) ":" amount))))
      (println "  Total Food:" (reduce + 0 (vals food))))
    (println)

    ;; Basic Resources
    (println "🪵 Basic Resources:")
    (println "  Wood:" (:wood (:resources overview)))
    (println "  Stone:" (:stone (:resources overview)))
    (println)

    ;; Materials
    (println "⚒️  Materials:")
    (println "  Coal:" (:coal (:materials overview)))
    (println "  Ore:" (:ore (:materials overview)))
    (println "  Metal:" (:metal (:materials overview)))
    (println "  Leather:" (:leather (:materials overview)))
    (println "  Clothes:" (:cloth (:materials overview)))
    (println)

    ;; Drinks
    (println "🍺 Drinks:")
    (println "  Wine:" (:wine (:drinks overview)))
    (println "  Beer:" (:beer (:drinks overview)))
    (println)

    ;; Buildings
    (println "🏠 Buildings:" (:total (:buildings overview)))
    (println)

    ;; Animals
    (println "🦌 Animals:")
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
         (when (pos? cold) (str " | ❄️ " cold " cold"))
         (when (pos? danger) (str " | 🚨 " danger " danger"))
         (when (zero? food-total) " | ⚠️ NO FOOD!")
         (when (pos? food-total) (str " | 🍞 " food-total " food")))))

(comment
  (quick-status)
  :rcf)

