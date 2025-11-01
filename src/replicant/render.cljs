(ns replicant.render
  (:require-macros [replicant.utils :refer [build-admin?]])
  (:require
   [replicant.navbar :refer [admin-navbar normal-navbar]] 
   #_[test2.render :refer [test2-page]]))

(comment
  (build-admin?)
  :rcf)

(defn page-layout [content]
  [:div {:class ["min-h-screen" "bg-base-100"]}
   (if (build-admin?)
     (admin-navbar)
     (normal-navbar))
   [:div {:class ["container" "px-4" "py-6"]}
    content]])

(defn render-frontpage []
  (page-layout
   [:h1 "首页"]))

(defn render-not-found [_]
  (page-layout
   [:h1 "Not found"]))

#_(defn render-test2 [state]
  (page-layout
   (test2-page state)))

(defn render-page [state]
  (let [f (case (:location/page-id (:location state))
            :pages/frontpage render-frontpage
            #_#_:pages/test2 render-test2
            render-not-found)]
    (f state)))
