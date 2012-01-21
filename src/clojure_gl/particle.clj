(ns clojure-gl.particle
  (:use (clojure-gl primitive)))

(defn random-between [min max]
  (let [range (- max min)
        rand (Math/random)]
    (+ min (* rand range))))

(defn random-angle []
  (random-between 0 360))

(defn fire-particle [pt]
  {:center pt
   :rotation (random-angle)
   :scale (random-between 0.1 0.2)
   :spin-rate (random-between -18 18)
   :scale-rate (random-between 0.1 0.3)
   :time 0
   :max-time (random-between 0.25 2.25)})

(defn updated-value [obj property delta-property dt]
  (+ (obj property) (* (obj delta-property dt))))

(defn update-particle [particle dt]
  (let [new-time (+ (particle :time) dt)]
    (if (< new-time (particle :max-time))
      (conj particle
            {:rotation (updated-value particle :rotation :spin-rate dt)
             :scale (updated-value particle :scale :scale-rate dt)
             :time new-time}))))

