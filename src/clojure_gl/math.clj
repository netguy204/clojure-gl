(ns clojure-gl.math
  (:use (clojure-gl buffers))
  (:import (org.lwjgl.util.vector Matrix4f Vector3f)))

(defn deg-to-rad [deg]
  (* Math/PI (/ deg 180.0)))

(defn rotation [theta rx ry rz]
  (.rotate (Matrix4f.) theta (Vector3f. rx ry rz)))

(defn scale [sx sy sz]
  (.scale (Matrix4f.) (Vector3f. sx sy sz)))

(defn translation [tx ty tz]
  (.translate (Matrix4f.) (Vector3f. tx ty tz)))

(defn mul2 [lhs rhs]
  (let [res (Matrix4f.)]
    (Matrix4f/mul lhs rhs res)
    res))

(defn mul [& many]
  (reduce mul2 (Matrix4f.) many))

(defn matrix-to-buffer [^Matrix4f mat]
  (let [^java.nio.FloatBuffer fb (create-float-buffer 16)]
    (.store mat fb)
    (.flip fb)
    fb))