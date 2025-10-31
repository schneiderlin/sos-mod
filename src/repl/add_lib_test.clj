(ns repl.add-lib-test
  (:require 
   [clojure.java.basis :as basis]
   [clojure.java.basis.impl :as basis-impl]
   [clojure.tools.deps :as deps]
   [clojure.repl.deps :refer [add-lib]]))

(comment
  (basis/initial-basis)
  (basis/current-basis)

  ;; 一定要有一个 basis 才可以 add-lib. 不是 clojure clj 启动的没有 basis
  ;; 如果是 java 启动, 可以用这个把 basis 加进去. 然后再 add-lib
  (basis-impl/update-basis! (constantly (deps/create-basis {})))

  (add-lib 'no.cjohansen/powerpack {:mvn/version "2025.10.22"})
  (add-lib 'datalevin/datalevin {:mvn/version "0.9.22"}) 
  (add-lib 'cheshire/cheshire {:mvn/version "6.1.0"})
  :rcf)
