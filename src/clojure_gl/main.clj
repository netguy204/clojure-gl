(set! *warn-on-reflection* true)

(ns clojure-gl.main
  (:use (clojure-gl core))
  (:gen-class))

(defn -main [& args]
  (run))