(ns clojure-gl.resources
  (:import (java.io BufferedReader InputStreamReader)))

(defn resource-as-stream [ref]
  (let [thr (Thread/currentThread)
        loader (.getContextClassLoader thr)]
    (if-let [stream (.getResourceAsStream loader ref)]
      stream
      (throw (RuntimeException. (format "couldn't find %s" ref))))))

(defn read-lines [ref]
  (line-seq (BufferedReader. (InputStreamReader. (resource-as-stream ref)))))

(defn resource-as-string [ref]
  (slurp (resource-as-stream ref)))