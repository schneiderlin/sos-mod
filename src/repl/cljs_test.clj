(ns repl.cljs-test
  (:require 
   [shadow.cljs.devtools.api :as shadow]
   [shadow.cljs.devtools.server :as shadow-server]))

(comment
  
  (shadow-server/start!)
  (shadow/watch :app)
  
  :rcf)
