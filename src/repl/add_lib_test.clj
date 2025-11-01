(ns repl.add-lib-test
  (:require 
   [clojure.java.basis :as basis]
   [clojure.java.basis.impl :as basis-impl]
   [clojure.tools.deps :as deps]
   [clojure.repl.deps :refer [add-lib add-libs]]))

(comment
  (basis/initial-basis)
  (basis/current-basis)

  ;; 一定要有一个 basis 才可以 add-lib. 不是 clojure clj 启动的没有 basis
  ;; 如果是 java 启动, 可以用这个把 basis 加进去. 然后再 add-lib
  (basis-impl/update-basis! 
   (constantly (merge (deps/create-basis {})
                      {:mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                                   "clojars" {:url "https://repo.clojars.org/"}}}))) 
  

  (add-libs '{ring/ring-core {:mvn/version "1.12.1"}
              ring/ring-jetty-adapter {:mvn/version "1.12.1"}
              ring/ring-json {:mvn/version "0.5.1"}
              ring-cors/ring-cors {:mvn/version "0.1.13"}
              metosin/reitit {:mvn/version "0.7.0"}
              metosin/reitit-ring {:mvn/version "0.7.2"}
              metosin/muuntaja {:mvn/version "0.6.11"}
              buddy/buddy-hashers {:mvn/version "2.0.167"}
              babashka/process {:mvn/version "0.6.23"}
              com.taoensso/telemere {:mvn/version "1.0.1"}
              com.taoensso/sente {:mvn/version "1.20.0"}
              clj-http/clj-http {:mvn/version "3.13.0"}
              com.domkm/silk {:mvn/version "0.1.2"}
              jarohen/chime {:mvn/version "0.3.3"}
              lambdaisland/uri {:mvn/version "1.19.155"}
              one-time/one-time {:mvn/version "0.8.0"}
              cheshire/cheshire {:mvn/version "5.10.1"}
              datalevin/datalevin {:git/url "https://github.com/juji-io/datalevin.git"
                                   :git/sha "7d2820b36bc242ffdd717764eee2036029927999"}
              org.slf4j/slf4j-simple {:mvn/version "2.0.9"}
              com.wsscode/pathom3 {:mvn/version "2025.01.16-alpha"}
              missionary/missionary {:mvn/version "b.46"}
              integrant/integrant {:mvn/version "1.0.1"}})
  
  (do
    (add-lib 'ring/ring-core {:mvn/version "1.12.1"})
    (add-lib 'ring/ring-jetty-adapter {:mvn/version "1.12.1"})
    (add-lib 'ring/ring-json {:mvn/version "0.5.1"})
    (add-lib 'ring-cors/ring-cors {:mvn/version "0.1.13"})
    (add-lib 'metosin/reitit {:mvn/version "0.7.0"})
    (add-lib 'metosin/reitit-ring {:mvn/version "0.7.2"})
    (add-lib 'metosin/muuntaja {:mvn/version "0.6.11"})
    (add-lib 'buddy/buddy-hashers {:mvn/version "2.0.167"})
    (add-lib 'babashka/process {:mvn/version "0.6.23"})
    (add-lib 'cheshire/cheshire {:mvn/version "5.10.1"}) 
    (add-lib 'datalevin/datalevin {:git/url "https://github.com/juji-io/datalevin.git"
                                   :git/sha "7d2820b36bc242ffdd717764eee2036029927999"})
    (add-lib 'org.slf4j/slf4j-simple {:mvn/version "2.0.9"})
    (add-lib 'com.wsscode/pathom3 {:mvn/version "2025.01.16-alpha"})
    (add-lib 'missionary/missionary {:mvn/version "b.46"})
    (add-lib 'integrant/integrant {:mvn/version "1.0.1"}))
  :rcf)
