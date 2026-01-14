(ns game.harvest
  (:require 
   [repl.utils :as utils])
  (:import 
   [settlement.main SETT]))

;; Get the clearing jobs system
(defn get-clearing-jobs []
  (.. SETT JOBS clearss))

(comment
  (get-clearing-jobs)
  :rcf)

;; Clear wood (trees) at a specific tile
;; tx, ty: tile coordinates
(defn clear-wood-once [tx ty]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           wood-job (.wood jobs)
           placer (.placer wood-job)]
       (.place placer tx ty nil nil)))))

;; Clear stone (rocks) at a specific tile
;; tx, ty: tile coordinates
(defn clear-stone-once [tx ty]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           stone-job (.stone jobs)
           placer (.placer stone-job)]
       (.place placer tx ty nil nil)))))

;; Clear both wood and stone at a specific tile
;; tx, ty: tile coordinates
(defn clear-wood-and-stone-once [tx ty]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           wood-job (.wood jobs)
           stone-job (.stone jobs)
           wood-placer (.placer wood-job)
           stone-placer (.placer stone-job)]
       ;; Try wood first, then stone
       (when (nil? (.isPlacable wood-placer tx ty nil nil))
         (.place wood-placer tx ty nil nil))
       (when (nil? (.isPlacable stone-placer tx ty nil nil))
         (.place stone-placer tx ty nil nil))))))

;; Clear wood in an area
;; start-x, start-y: top-left corner
;; width, height: dimensions
(defn clear-wood-area-once [start-x start-y width height]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           wood-job (.wood jobs)
           placer (.placer wood-job)]
       (doseq [y (range height)
               x (range width)]
         (let [tx (+ start-x x)
               ty (+ start-y y)]
           (when (nil? (.isPlacable placer tx ty nil nil))
             (.place placer tx ty nil nil))))))))

;; Clear stone in an area
;; start-x, start-y: top-left corner
;; width, height: dimensions
(defn clear-stone-area-once [start-x start-y width height]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           stone-job (.stone jobs)
           placer (.placer stone-job)]
       (doseq [y (range height)
               x (range width)]
         (let [tx (+ start-x x)
               ty (+ start-y y)]
           (when (nil? (.isPlacable placer tx ty nil nil))
             (.place placer tx ty nil nil))))))))

;; Clear both wood and stone in an area
;; start-x, start-y: top-left corner
;; width, height: dimensions
(defn clear-wood-and-stone-area-once [start-x start-y width height]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           wood-job (.wood jobs)
           stone-job (.stone jobs)
           wood-placer (.placer wood-job)
           stone-placer (.placer stone-job)]
       (doseq [y (range height)
               x (range width)]
         (let [tx (+ start-x x)
               ty (+ start-y y)]
           (when (nil? (.isPlacable wood-placer tx ty nil nil))
             (.place wood-placer tx ty nil nil))
           (when (nil? (.isPlacable stone-placer tx ty nil nil))
             (.place stone-placer tx ty nil nil))))))))

;; Forage wild crops (采集野生作物) at a specific tile
;; tx, ty: tile coordinates
(defn forage-crop-once [tx ty]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           food-job (.food jobs)
           placer (.placer food-job)]
       (.place placer tx ty nil nil)))))

;; Forage wild crops in an area
;; start-x, start-y: top-left corner
;; width, height: dimensions
(defn forage-crop-area-once [start-x start-y width height]
  (utils/update-once
   (fn [_ds]
     (let [jobs (get-clearing-jobs)
           food-job (.food jobs)
           placer (.placer food-job)]
       (doseq [y (range height)
               x (range width)]
         (let [tx (+ start-x x)
               ty (+ start-y y)]
           (when (nil? (.isPlacable placer tx ty nil nil))
             (.place placer tx ty nil nil))))))))

(comment
  ;; Example usage:
  ;; Clear wood at a single tile
  (clear-wood-once 259 437)
  
  ;; Clear stone at a single tile
  (clear-stone-once 279 418)
  
  ;; Clear both at a single tile
  (clear-wood-and-stone-once 100 100)
  
  ;; Clear wood in a 10x10 area
  (clear-wood-area-once 259 437 5 5)
  
  ;; Clear stone in a 10x10 area
  (clear-stone-area-once 297 525 30 30)
  
  ;; Clear both in a 10x10 area
  (clear-wood-and-stone-area-once 100 100 10 10)
  
  ;; Forage wild crops at a single tile
  (forage-crop-once 230 608)
  
  ;; Forage wild crops in an area
  (forage-crop-area-once 230 608 10 10)
  :rcf)

