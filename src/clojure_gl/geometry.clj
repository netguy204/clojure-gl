(ns clojure-gl.geometry
  (:use (clojure-gl buffers))
  (:import ()))

(defn create-unit-quad []
  (let [verts (gl-point-buffer 3 [[-0.5 -0.5 0.0]
                                  [-0.5 0.5 0.0]
                                  [0.5 0.5 0.0]
                                  [0.5 -0.5 0.0]])
        texcoords (gl-point-buffer 2 [[0 0]
                                      [0 1]
                                      [1 1]
                                      [1 0]])]
    {:verts verts
     :texcoords texcoords}))
