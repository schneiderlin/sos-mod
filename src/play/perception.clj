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
        '[settlement.stats.standing STANDINGS StandingCitizen StandingSlave]
        '[settlement.stats.colls StatsPopulation StatsFood StatsEnv StatsAccess StatsReligion StatsWork StatsGovern]
        '[settlement.stats.service StatsService StatServiceRoom]
        '[settlement.stats.law StatsLaw]
        '[game.faction FACTIONS]
        '[init.type HCLASSES HTYPES]
        '[init.race RACES]
        '[init.type NEEDS]
        '[init.type POP_CL])

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

;; ============================================================================
;; Loyalty and Happiness Stats
;; ============================================================================

(defn loyalty-status
  "Get loyalty information for citizens.
   Returns a map with overall loyalty, target loyalty, and fulfillment."
  []
  (let [citizen-standings (STANDINGS/CITIZEN)
        fulfillment (.-fullfillment citizen-standings)
        happiness (.-happiness citizen-standings)]
    {:overall (.current citizen-standings)
     :target (.target citizen-standings)
     :fulfillment (into {}
                        (map (fn [race]
                               [(.-key race)
                                (.getD fulfillment race)])
                             (array-list->vec (RACES/all))))
     :happiness (into {}
                      (map (fn [race]
                             [(.-key race)
                              (.getD happiness race)])
                           (array-list->vec (RACES/all))))}))

(comment
  (loyalty-status)
  :rcf)

(defn happiness-status
  "Get happiness metrics for the population.
   Returns a map with happiness values by race."
  []
  (let [citizen-standings (STANDINGS/CITIZEN)
        happiness (.-happiness citizen-standings)
        fulfillment (.-fullfillment citizen-standings)]
    {:by-race (into {}
                    (map (fn [race]
                           [(.-key race)
                            {:happiness (.getD happiness race)
                             :fulfillment (.getD fulfillment race)}])
                         (array-list->vec (RACES/all))))}))

(comment
  (happiness-status)
  :rcf)

;; ============================================================================
;; Population Breakdown
;; ============================================================================

(defn population-breakdown
  "Get detailed population information by class, race, and type."
  []
  (let [pop-stats (STATS/POP)
        all-hclasses (array-list->vec (HCLASSES/ALL))
        all-races (array-list->vec (RACES/all))
        all-htypes [HTYPES/SUBJECT HTYPES/RETIREE HTYPES/RECRUIT HTYPES/STUDENT
                    HTYPES/PRISONER HTYPES/SLAVE HTYPES/CHILD]]
    {:by-class (into {}
                     (map (fn [hclass]
                            [(.toString (.name hclass))
                             (into {}
                                   (map (fn [race]
                                          [(.-key race)
                                           (.get (.data (.-POP pop-stats) hclass) race 0)])
                                        all-races))])
                          (filter #(.player %) all-hclasses)))
     :by-race (into {}
                    (map (fn [race]
                           [(.-key race)
                            (.get (.data (.-POP pop-stats) nil) race 0)])
                         all-races))
     :by-type (into {}
                    (map (fn [htype]
                           [(.-key htype)
                            (.pop pop-stats htype)])
                         (map #(%) all-htypes)))
     :total (.get (.data (.-POP pop-stats) nil) nil 0)
     :age-stats {:days (.-AGE_DAYS (.-age pop-stats))
                 :demography (.demography pop-stats)}
     :status {:trapped (.-TRAPPED pop-stats)
              :emigrating (.-EMMIGRATING pop-stats)
              :majority (.-MAJORITY pop-stats)
              :slaves-self (.-SLAVES_SELF pop-stats)
              :slaves-other (.-SLAVES_OTHER pop-stats)}}))

(comment
  (population-breakdown)
  :rcf)

;; ============================================================================
;; Service Facilities Coverage
;; ============================================================================

(defn service-coverage
  "Get service facility access and coverage information.
   Returns a map with service stats for all service rooms."
  []
  (let [service-stats (STATS/SERVICE)
        all-services (array-list->vec (.-ROOMS service-stats))
        all-hclasses (filter #(.player %) (array-list->vec (HCLASSES/ALL)))
        all-races (array-list->vec (RACES/all))]
    {:services (into {}
                     (map (fn [service]
                            (let [name (.toString (.-name service))
                                  access-data (.access service)
                                  upgrade-data (.upgrade service)
                                  quality-data (.quality service)
                                  proximity-data (.proximity service)]
                              [name
                               {:access (into {}
                                            (map (fn [hclass]
                                                   [(.toString (.name hclass))
                                                    (into {}
                                                          (map (fn [race]
                                                                 [(.toString (.-key race))
                                                                  (.getD (.data access-data hclass) race)])
                                                               all-races))])
                                                 all-hclasses))
                                :upgrade (into {}
                                             (map (fn [hclass]
                                                    [(.toString (.name hclass))
                                                     (into {}
                                                           (map (fn [race]
                                                                  [(.toString (.-key race))
                                                                   (.getD (.data upgrade-data hclass) race)])
                                                                all-races))])
                                                  all-hclasses))
                                :quality (into {}
                                             (map (fn [hclass]
                                                    [(.toString (.name hclass))
                                                     (into {}
                                                           (map (fn [race]
                                                                  [(.toString (.-key race))
                                                                   (.getD (.data quality-data hclass) race)])
                                                                all-races))])
                                                  all-hclasses))
                                :proximity (into {}
                                               (map (fn [hclass]
                                                      [(.toString (.name hclass))
                                                       (into {}
                                                             (map (fn [race]
                                                                    [(.toString (.-key race))
                                                                     (.getD (.data proximity-data hclass) race)])
                                                                  all-races))])
                                                    all-hclasses))}]))
                          all-services))
     :need-totals (into {}
                        (map (fn [need]
                               [(.toString need)
                                (.needTot service-stats need)])
                             (array-list->vec (NEEDS/ALL))))}))

(comment
  (service-coverage)
  :rcf)

;; ============================================================================
;; Environment Status
;; ============================================================================

(defn environment-status
  "Get environmental impact information including pollution, beauty, etc."
  []
  (let [env-stats (STATS/ENV)
        env-map (.-map (settlement.main.SETT/ENV))
        access-stats (STATS/ACCESS)
        access-coll (.ACCESS access-stats)  ; StatsA nested class
        all-stats (array-list->vec (.all access-coll))  ; List of STAT objects
        all-hclasses (filter #(.player %) (array-list->vec (HCLASSES/ALL)))
        all-races (array-list->vec (RACES/all))
        ;; Helper to get stat by SettEnv field (e.g., NOISE, LIGHT)
        get-env-stat (fn [sett-env]
                       (let [env-idx (.index sett-env)]
                         (.get all-stats env-idx)))]
    {:preferences {:building (.-BUILDING_PREF env-stats)
                   :road (.-ROAD_PREF env-stats)
                   :pool (.-POOL_PREF env-stats)
                   :climate (.-CLIMATE env-stats)
                   :others (.-OTHERS env-stats)}
     :pollution {:cannibalism (.-CANNIBALISM env-stats)
                 :unburied (.-UNBURRIED env-stats)}
     :access {:noise (get-env-stat (.-NOISE env-map))
              :light (get-env-stat (.-LIGHT env-map))
              :space (get-env-stat (.-SPACE env-map))
              :water-sweet (get-env-stat (.-WATER_SWEET env-map))
              :water-salt (get-env-stat (.-WATER_SALT env-map))
              :urban (get-env-stat (.-URBAN env-map))}
     :road-access (.-ACCESS_ROAD env-stats)
     :by-class (into {}
                     (map (fn [hclass]
                            [(.toString (.name hclass))
                             {:building-preference (.getD (.data (.-BUILDING_PREF env-stats) hclass) nil)
                              :road-access (.getD (.data (.-ACCESS_ROAD env-stats) hclass) nil)
                              :climate-suitability (.getD (.data (.-CLIMATE env-stats) hclass) nil)}])
                          all-hclasses))
     :by-race (into {}
                    (map (fn [race]
                           [(.toString (.-key race))
                            {:building-preference (.getD (.data (.-BUILDING_PREF env-stats) nil) race)
                             :road-preference (.getD (.data (.-ROAD_PREF env-stats) nil) race)
                             :pool-preference (.getD (.data (.-POOL_PREF env-stats) nil) race)
                             :others-preference (.getD (.data (.-OTHERS env-stats) nil) race)}])
                         all-races))}))

(comment
  (environment-status)
  :rcf)

;; ============================================================================
;; Religion Status
;; ============================================================================

(defn religion-status
  "Get religious satisfaction and temple coverage information."
  []
  (let [religion-stats (STATS/RELIGION)
        all-religions (array-list->vec (.-ALL religion-stats))
        all-hclasses (filter #(.player %) (array-list->vec (HCLASSES/ALL)))
        all-races (array-list->vec (RACES/all))]
    {:by-religion (into {}
                        (map (fn [religion]
                               [(.-key (.-religion religion))
                                {:temple-access (.getD (.data (.access (.-TEMPLE religion-stats) (.-religion religion))) nil)
                                 :temple-quality (.getD (.data (.quality (.-TEMPLE religion-stats) (.-religion religion))) nil)
                                 :shrine-access (.getD (.data (.access (.-SHRINE religion-stats) (.-religion religion))) nil)
                                 :shrine-quality (.getD (.data (.quality (.-SHRINE religion-stats) (.-religion religion))) nil)
                                 :followers (.-followers religion)
                                 :total (.getD (.data (.-followers religion)) nil)}])
                             all-religions))
     :opposition (.getD (.data (.-OPPOSITION religion-stats)) nil)
     :by-class (into {}
                     (map (fn [hclass]
                            [(.toString (.name hclass))
                             (into {}
                                   (map (fn [religion]
                                          [(.-key (.-religion religion))
                                           {:temple-access (.getD (.data (.access (.-TEMPLE religion-stats) (.-religion religion)) hclass) nil)
                                            :temple-quality (.getD (.data (.quality (.-TEMPLE religion-stats) (.-religion religion)) hclass) nil)
                                            :shrine-access (.getD (.data (.access (.-SHRINE religion-stats) (.-religion religion)) hclass) nil)
                                            :shrine-quality (.getD (.data (.quality (.-SHRINE religion-stats) (.-religion religion)) hclass) nil)}])
                                        all-religions))])
                          all-hclasses))
     :by-race (into {}
                    (map (fn [race]
                           [(.toString (.-key race))
                            (into {}
                                  (map (fn [religion]
                                         [(.-key (.-religion religion))
                                          {:temple-access (.getD (.data (.access (.-TEMPLE religion-stats) (.-religion religion)) nil) race)
                                           :temple-quality (.getD (.data (.quality (.-TEMPLE religion-stats) (.-religion religion)) nil) race)
                                           :shrine-access (.getD (.data (.access (.-SHRINE religion-stats) (.-religion religion)) nil) race)
                                           :shrine-quality (.getD (.data (.quality (.-SHRINE religion-stats) (.-religion religion)) nil) race)}])
                                      all-religions))])
                         all-races))}))

(comment
  (religion-status)
  :rcf)

;; ============================================================================
;; Employment Status
;; ============================================================================

(defn employment-status
  "Get employment, job satisfaction, and unemployment information."
  []
  (let [work-stats (STATS/WORK)
        all-races (array-list->vec (RACES/all))]
    {:workforce {:total (.workforce work-stats)
                 :by-race (into {}
                                (map (fn [race]
                                       [(.-key race)
                                        (.workforce work-stats race)])
                                     all-races))}
     :fulfillment {:total (.getD (.data (.-WORK_FULFILLMENT work-stats)) nil)
                   :by-race (into {}
                                  (map (fn [race]
                                         [(.-key race)
                                          (.getD (.data (.-WORK_FULFILLMENT work-stats) nil) race)])
                                       all-races))}
     :retirement {:age (.-RETIREMENT_AGE (.-RET work-stats))
                  :home-access (.-RETIREMENT_HOME (.-RET work-stats))}
     :work-time (.-WORK_TIME work-stats)}))

(comment
  (employment-status)
  :rcf)

;; ============================================================================
;; Government and Law Status
;; ============================================================================

(defn government-status
  "Get government, law, and policy information including taxes, laws, administration."
  []
  (let [govern-stats (STATS/GOVERN)
        law-stats (STATS/LAW)
        llaw (.-lLAW law-stats)
        ;; Get LawRate via LAW.law() static method
        law-rate (.rate (settlement.stats.law.LAW/law))
        ;; Get Processing for punishment limits
        processing (settlement.stats.law.LAW/process)
        all-races (array-list->vec (RACES/all))]
    {:wealth {:riches (.-RICHES govern-stats)
              :tourism-friend (.-tourismFriend govern-stats)
              :tourism-enemy (.-tourismEnemy govern-stats)}
     :law {:equality (.-EQUALITY law-stats)
           :effectiveness (.getD law-rate 0)
           :ex-con (.getD (.data (.-EX_CON law-stats) nil) nil)}
     :punishments (array-list->vec (.-punishments law-stats))
     :punishment-limits (into {}
                               (map (fn [p]
                                      [(.-key p)
                                       (into {}
                                             (map (fn [race]
                                                    [(.toString (.-key race))
                                                     (.limit p race)])
                                                  all-races))])
                                    (array-list->vec (.-punishmentsdec processing))))}))

;; ============================================================================
;; Food Distribution (Rations) Status
;; ============================================================================

(defn distribution-status
  "Get food and drink ration distribution information by class and race."
  []
  (let [food-stats (STATS/FOOD)
        food-decree (.decree (.-FOOD food-stats))
        drink-decree (.decree (.-DRINK food-stats))
        all-hclasses (filter #(.player %) (array-list->vec (HCLASSES/ALL)))
        all-races (array-list->vec (RACES/all))]
    {:food-rations {:decree food-decree
                    :by-class (into {}
                                   (map (fn [hclass]
                                          [(.toString (.name hclass))
                                           (into {}
                                                 (map (fn [race]
                                                        [(.toString (.-key race))
                                                         (.get food-decree hclass race)])
                                                      all-races))])
                                        all-hclasses))
                    :days-remaining (.-FOOD_DAYS food-stats)}
     :drink-rations {:decree drink-decree
                     :by-class (into {}
                                    (map (fn [hclass]
                                           [(.toString (.name hclass))
                                            (into {}
                                                  (map (fn [race]
                                                         [(.toString (.-key race))
                                                          (.get drink-decree hclass race)])
                                                       all-races))])
                                         all-hclasses))}
     :starvation (.-STARVATION food-stats)}))

(comment
  (distribution-status)
  :rcf)

;; ============================================================================
;; Combined Settlement Stats Summary
;; ============================================================================

(defn settlement-stats-summary
  "Get a comprehensive summary of all settlement stats including loyalty, happiness,
   population, services, environment, religion, employment, and government."
  []
  {:loyalty (loyalty-status)
   :happiness (happiness-status)
   :population (population-breakdown)
   :distribution (distribution-status)
   :services (service-coverage)
   :environment (environment-status)
   :religion (religion-status)
   :employment (employment-status)
   :government (government-status)})

(comment
  (settlement-stats-summary)
  :rcf)

(defn print-settlement-stats-summary
  "Print a human-readable summary of all settlement stats."
  []
  (let [summary (settlement-stats-summary)]
    (println "=== SETTLEMENT STATS SUMMARY ===")
    (println)

    ;; Loyalty
    (println "Loyalty:")
    (println "  Overall:" (format "%.2f" (:overall (:loyalty summary))))
    (println "  Target:" (format "%.2f" (:target (:loyalty summary))))
    (println)

    ;; Population
    (println "Population:")
    (println "  Total:" (:total (:population summary)))
    (println "  By Class:" (:by-class (:population summary)))
    (println "  By Race:" (:by-race (:population summary)))
    (println)

    ;; Employment
    (println "Employment:")
    (println "  Employed:" (:total (:employment (:employment summary))))
    (let [workforce-total (:total (:workforce (:employment summary)))
          employed-total (:total (:employment (:employment summary)))]
      (print "  Workforce:" workforce-total)
      (when (pos? workforce-total)
        (println " (" (format "%.1f" (* 100 (/ employed-total workforce-total))) "%)")
        (println)))
    (println "  Incapacitated:" (:total (:incapacitated (:employment summary))))
    (println)

    ;; Services (simplified)
    (println "Services: " (count (:services (:services summary))) "service types available")
    (println)

    ;; Environment
    (println "Environment:")
    (println "  Unburied Corpses:" (:unburied (:pollution (:environment summary))))
    (println)

    ;; Religion
    (println "Religion:")
    (println "  Opposition:" (format "%.2f" (:opposition (:religion summary))))
    (println)

    ;; Government
    (println "Government:")
    (println "  Riches:" (format "%.2f" (:riches (:wealth (:government summary))))
    (println "  Law Rate:" (format "%.2f" (:rate (:law (:government summary)))))

    summary)))
