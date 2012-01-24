(ns clojure-gl.core
  (:use (clojure-gl prepare texture primitive
                    particle buffers shaders
                    geometry math))
  (:import (org.lwjgl LWJGLException)
           (org.lwjgl.opengl Display GL11 GL12 GL14 GL15 GL20 GL21)
           (org.lwjgl.util.vector Matrix4f)))

(defn start-thread [runnable]
  (.start (Thread. runnable)))

(def ^:dynamic *width* nil)
(def ^:dynamic *height* nil)
(def ^:dynamic *aspect-ratio* nil)
(def ^:dynamic *texture-cache* nil)
(def ^:dynamic *program-cache* nil)
(def ^:dynamic *unit-quad* nil)
(def ^:dynamic *screen-pbo* nil)
(def ^:dynamic *screen-texture* nil)

(def guy-texture "clojure-gl/guy.png")
(def fire-texture "clojure-gl/fire.png")
(def star-texture "clojure-gl/star.png")

(def identity-program
  {:name "identity-program"
   :shaders {:vertex "clojure-gl/identity.vs"
             :fragment "clojure-gl/identity.fs"}
   :attributes [[*attribute-vertex* "vVertex"]
                [*attribute-texture-coords* "vTexCoord0"]]})

(def num-particles 150)

(defn game-state-init []
  {:time 0
   :fires (for [x (range num-particles)] (fire-particle [0.0 0.0]))})

(defn render-particles [game-state fixed-alpha]
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (let [[^Integer program cache] (get-program identity-program *program-cache*)
        texture-binding (GL20/glGetUniformLocation program "textureUnit0")
        transform-binding (GL20/glGetUniformLocation program "mvMatrix")
        alpha-binding (GL20/glGetUniformLocation program "alpha")
        time (/ (game-state :time) 1000)
        rotation-factor (* time 0.5)
        scale-factor (+ 0.75 (* 0.25 (Math/sin (* time 0.25))))
        world-transform (mul (scale scale-factor scale-factor scale-factor)
                             (translation (Math/cos rotation-factor)
                                          (Math/sin rotation-factor) 0.0))]
    (GL20/glUseProgram program)
    (GL20/glUniform1i texture-binding 0)
    (gl-bind-buffer (*unit-quad* :verts) 3 *attribute-vertex*)
    (gl-bind-buffer (*unit-quad* :texcoords) 2 *attribute-texture-coords*)

    (doseq [particle (game-state :fires)]
      (let [rot (rotation (deg-to-rad (particle :rotation)) 0.0 0.0 1.0)
            sf (particle :scale)
            sc (scale sf sf sf)
            tf (mul world-transform rot sc (translation 1.0 0.0 0.0))]
        (GL20/glUniformMatrix4 transform-binding false (matrix-to-buffer tf))
        (GL20/glUniform1f alpha-binding (if fixed-alpha fixed-alpha (exp-alpha particle)))
        (GL11/glDrawArrays GL11/GL_TRIANGLE_FAN 0 4)))))

(defn game-cycle [dtms game-state]

  (bind-texture (*texture-cache* star-texture))
  (render-particles game-state false)

  ;; render into a PBO
  (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER *screen-pbo*)
  (GL11/glReadPixels 0 0 *width* *height* GL11/GL_RGBA GL11/GL_UNSIGNED_BYTE 0)
  (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER 0)

  ;; put the render result into a texture
  (GL15/glBindBuffer GL21/GL_PIXEL_UNPACK_BUFFER *screen-pbo*)
  (bind-texture *screen-texture*)
  (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR)
  (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR)
  (GL11/glTexImage2D GL11/GL_TEXTURE_2D 0 GL11/GL_RGBA *width* *height* 0 GL11/GL_RGBA GL11/GL_UNSIGNED_BYTE 0)
  (GL15/glBindBuffer GL21/GL_PIXEL_UNPACK_BUFFER 0)

  ;; and draw the particles again using that render result
  (render-particles game-state 0.6)

  
  (Display/update)
  (Display/sync 30)

  (conj game-state
        {:time (+ (game-state :time) dtms)
         :fires (for [particle (game-state :fires)]
                  (if-let [particle (update-particle particle (/ dtms 1000.0))]
                    particle
                    (fire-particle [0.0 0.0])))}))

(defn should-exit []
  (Display/isCloseRequested))

(defn game-loop []
  (loop [last-time (System/currentTimeMillis)
         game-state (game-state-init)]
    (let [current-time (System/currentTimeMillis)
          next-game-state (#'game-cycle (- current-time last-time) game-state)]
      (if-not (should-exit)
        (recur current-time next-game-state)))))

(defn init-gl []
  (GL11/glMatrixMode GL11/GL_PROJECTION)
  (GL11/glLoadIdentity)
  (GL11/glOrtho 0 *aspect-ratio* 0 1 1 -1)
  (GL11/glMatrixMode GL11/GL_MODELVIEW)
  (GL11/glEnable GL11/GL_TEXTURE_2D)
  (GL11/glDisable GL11/GL_DEPTH_TEST)
  (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
  (GL11/glEnable GL11/GL_BLEND))

(defn find-mode [width height]
  (first (filter (fn [m]
                   (and (== (.getWidth m) width)
                        (== (.getHeight m) height)))
                 (Display/getAvailableDisplayModes))))

(defn run []
  (Display/setTitle "Hello World")
  (Display/setDisplayMode (find-mode 800 600))
  (Display/create)

  (try
    (binding [*width* (Display/getWidth)
              *height* (Display/getHeight)
              *aspect-ratio* (/ (Display/getWidth) (Display/getHeight))
              *texture-cache* (preload-textures nil guy-texture fire-texture star-texture)
              *program-cache* (preload-programs nil identity-program)
              *unit-quad* (create-unit-quad)
              *screen-pbo* (gl-buffer)
              *screen-texture* (create-texture-id)]
      ;; allocate space for the screen pbo
      (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER *screen-pbo*)
      (GL15/glBufferData GL21/GL_PIXEL_PACK_BUFFER (* *width* *height* 4) GL15/GL_DYNAMIC_COPY)
      (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER 0)
      
      (init-gl)
      (game-loop))

    (finally (Display/destroy))))

