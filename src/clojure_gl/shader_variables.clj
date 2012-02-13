(ns clojure-gl.shader-variables)

(defprotocol ActsLikeUniform
  "A thing that can be bound to a uniform slot of a shader program"
  (uniform-bind [value location]))

