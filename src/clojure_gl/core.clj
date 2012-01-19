(ns clojure-gl.core
  (:use (clojure-gl prepare))
  (:import (org.lwjgl LWJGLException)
           (org.lwjgl.opengl Display)))

(defn start-thread [runnable]
  (.start (Thread. runnable)))

(defonce exit-requested (ref false))

(defn till-false [runnable]
  (loop []
    (if (runnable)
      (recur))))

(defn exit-checker []
  (Thread/sleep 100)
  (if (not (Display/isCloseRequested))
    true
    (do
      (dosync (ref-set exit-requested true))
      false)))

(defn start-exit-checker []
  (dosync (ref-set exit-requested false))
  (start-thread
   (till-false exit-checker)))

(defn game-cycle []
  )

(defn game-loop []
  (loop []
    (game-cycle)
    (if (not exit-requested)
      (recur))))

(defn run []
  (Display/setTitle "Hello World")
  (Display/create)

  (start-exit-checker)
  (game-loop)

  (Display/destroy))

