(ns game.settlement
  "Settlement-level entity queries and operations.
   Provides access to all entities in the settlement."
  (:import
   [settlement.main SETT]))

(defn all-entities
  "Get ALL entities in the settlement (humanoids, animals, objects, etc.).
   Returns non-nil entities only."
  []
  (let [entities-instance (SETT/ENTITIES)
        entities (.getAllEnts entities-instance)
        non-nil-entities (filter #(not (nil? %)) entities)]
    non-nil-entities))

(comment
  (all-entities)
  :rcf)
