(ns clojure-gl.geometry
  (:use (clojure-gl buffers))
  (:import ()))

(defn create-unit-quad []
  (let [verts (gl-point-buffer 3 [[-0.5 -0.5 0.0]
                                  [-0.5 0.5 0.0]
                                  [0.5 0.5 0.0]
                                  [0.5 -0.5 0.0]])
        texcoords (gl-point-buffer 2 [[0 1]
                                      [0 0]
                                      [1 0]
                                      [1 1]])]
    {:verts verts
     :texcoords texcoords}))


(defn verts-to-triangles [triangle-verts]
  (for [tri (range (/ (count triangle-verts) 3))]
    (for [vert (range 3)]
      (triangle-verts (+ (* tri 3) vert)))))

(defn third [seq]
  (nth seq 2))

(defn cross [a b]
  (let [a1 (first a)
        a2 (second a)
        a3 (third a)

        b1 (first b)
        b2 (second b)
        b3 (third b)]
    [(- (* a2 b3) (* a3 b2))
     (- (* a3 b1) (* a1 b3))
     (- (* a1 b2) (* a2 b1))]))

(defn triangles-to-normals [triangles]
  (for [tri (verts-to-triangles triangles)]
    (let [a (first tri)
          b (second tri)
          c (third tri)
          ab (map - b a)
          bc (map - c b)]
      (cross ab bc))))

(defn triple
  ([seq n]
     (if (= n 2)
       (triple (rest seq))
       (lazy-seq
         (cons (first seq) (triple seq (+ n 1))))))
  ([coll]
     (lazy-seq
       (when-let [s (seq coll)]
         (cons (first s) (triple s 0))))))