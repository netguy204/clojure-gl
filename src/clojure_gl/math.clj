(ns clojure-gl.math
  (:use (clojure-gl buffers shader-variables))
  (:import (org.lwjgl.util.vector Matrix4f Vector3f)
           (org.lwjgl.opengl GL20)))

(defn deg-to-rad [deg]
  (* Math/PI (/ deg 180.0)))

(defn eye []
  (Matrix4f.))

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

(defn matrix-to-buffer [^Matrix4f mat & buffer]
  (let [^java.nio.FloatBuffer fb (if buffer
                                   (first buffer)
                                   (create-float-buffer 16))]
    (.store mat fb)
    (.flip fb)
    fb))

(def ^:dynamic *matrix-buffer* nil)

(extend Matrix4f
  ActsLikeUniform
  {:uniform-bind (fn [value location]
                   (if *matrix-buffer*
                     (GL20/glUniformMatrix4 location false (matrix-to-buffer value *matrix-buffer*))
                     (GL20/glUniformMatrix4 location false (matrix-to-buffer value))))})

(extend Double
  ActsLikeUniform
  {:uniform-bind (fn [value location]
                   (GL20/glUniform1f location value))})

(extend Long
  ActsLikeUniform
  {:uniform-bind (fn [value location]
                   (GL20/glUniform1i location value))})