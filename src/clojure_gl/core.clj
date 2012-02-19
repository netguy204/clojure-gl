(ns clojure-gl.core
  (:use (clojure-gl prepare texture primitive
                    particle buffers shaders
                    shader-variables geometry math
                    marching-cubes))
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
(def ^:dynamic *simplex-surface* nil)
(def ^:dynamic *screen-pbo* nil)
(def ^:dynamic *screen-texture* nil)
(def ^:dynamic *test-float-texture* nil)

(def guy-texture "clojure-gl/guy.png")
(def fire-texture "clojure-gl/fire.png")
(def star-texture "clojure-gl/star.png")

(def identity-program
  {:name "identity-program"
   :shaders {:vertex "clojure-gl/identity.vs"
             :fragment "clojure-gl/identity.fs"}
   :attributes {:vertices {:name "vVertex"
                           :arity 3}
                :texture-coords {:name "vTexCoord0"
                                 :arity 2}}
   :uniforms {:texture-unit "textureUnit0"
              :mv-matrix "mvMatrix"
              :alpha "alpha"}})

(def no-texture-program
  {:name "no-texture-program"
   :shaders {:vertex "clojure-gl/no-texture.vs"
             :fragment "clojure-gl/no-texture.fs"}
   :attributes {:vertices {:name "vVertex"
                           :arity 3}
                :normals {:name "normal"
                          :arity 3}}
   :uniforms {:mv-matrix "mvMatrix"}})

(def num-particles 350)

(defn game-state-init []
  {:time 0
   :fires (for [x (range num-particles)] (fire-particle [0.0 0.0]))})

(defn render-particles [game-state fixed-alpha]
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (let [[program _] (get-program identity-program *program-cache*)
        time (/ (game-state :time) 1000.0)
        rotation-factor (* time 0.5)
        scale-factor (+ 0.75 (* 0.25 (Math/sin (* time 0.25))))
        world-transform (mul (scale scale-factor scale-factor scale-factor)
                             (translation (Math/cos rotation-factor)
                                          (Math/sin rotation-factor) 0.0))]
    (use-program program)
    (bind-program-attribute program :vertices (*unit-quad* :verts))
    (bind-program-attribute program :texture-coords (*unit-quad* :texcoords))
    (bind-program-uniform program :texture-unit 0)

    (doseq [particle (game-state :fires)]
      (let [rot (rotation (deg-to-rad (particle :rotation)) 0.0 0.0 1.0)
            sf (particle :scale)
            sc (scale sf sf sf)
            tf (mul world-transform rot sc (translation 1.0 0.0 0.0))]

        (bind-program-uniform program :mv-matrix tf)
        (bind-program-uniform program :alpha (if fixed-alpha fixed-alpha (exp-alpha particle)))

        (GL11/glDrawArrays GL11/GL_TRIANGLE_FAN 0 4)))))

(defn cycle2 [dtms game-state]
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (let [[program _] (get-program no-texture-program *program-cache*)]
    (use-program program)
    (bind-program-attribute program :vertices (*simplex-surface* :buffer))
    (bind-program-attribute program :normals (*simplex-surface* :normals))
  
    (let [time (/ (game-state :time) 1000.0)
          rotation-factor1 (* time 0.5)
          rotation-factor2 (* time 0.25)
          mat (mul (scale 0.5 0.5 0.5)
                   (rotation rotation-factor1 0.0 1.0 0.0)
                   (rotation rotation-factor2 0.0 0.0 1.0))]
      (bind-program-uniform program :mv-matrix mat)
      (GL11/glDrawArrays GL11/GL_TRIANGLES 0 (*simplex-surface* :vertex-count)))

    (Display/update)
    (Display/sync 30)
    (conj game-state {:time (+ (game-state :time) dtms)})))

(defn game-cycle [dtms game-state]

  (bind-texture (*texture-cache* fire-texture))
  (render-particles game-state false)

  ;; render into a PBO
  (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER *screen-pbo*)
  (GL11/glReadPixels (int 0) (int 0) (int *width*) (int *height*)
                     GL12/GL_BGRA GL12/GL_UNSIGNED_INT_8_8_8_8_REV (long 0))
  (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER 0)

  ;; put the render result into a texture
  (GL15/glBindBuffer GL21/GL_PIXEL_UNPACK_BUFFER *screen-pbo*)
  (bind-texture *screen-texture*)
  (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR)
  (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR)
  (GL11/glTexImage2D GL11/GL_TEXTURE_2D (int 0) GL11/GL_RGBA (int *width*) (int *height*)
                     (int 0) GL12/GL_BGRA GL12/GL_UNSIGNED_INT_8_8_8_8_REV (long 0))
  (GL15/glBindBuffer GL21/GL_PIXEL_UNPACK_BUFFER 0)

  ;; and draw the particles again using that render result
  (render-particles game-state false)

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
          next-game-state (#'cycle2 (- current-time last-time) game-state)]
      (if-not (should-exit)
        (recur current-time next-game-state)))))

(defn init-gl []
  (GL11/glViewport 0 0 *width* *height*)

  (GL11/glEnable GL11/GL_TEXTURE_2D)
  (GL11/glEnable GL11/GL_DEPTH_TEST)
  (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
  (GL11/glEnable GL11/GL_BLEND))

(defn find-mode [width height]
  (first (filter (fn [m]
                   (and (== (.getWidth m) width)
                        (== (.getHeight m) height)))
                 (Display/getAvailableDisplayModes))))

(defn simplex-texture [width height]
  (let [w-over-two (/ width 2)
        h-over-two (/ height 2)
        two-over-w (/ 2.0 width)
        two-over-h (/ 2.0 height)]
    (for [y (range (- h-over-two) h-over-two)]
      (for [x (range (- w-over-two) w-over-two)]
        (let [value (simplex-noise (* two-over-w x) (* two-over-h y) 0.35)]
          [value value value 1.0])))))

(defn run []
  (Display/setTitle "Hello World")
  (Display/setDisplayMode (find-mode 1024 768))
  (Display/create)

  (try
    (binding [*width* (Display/getWidth)
              *height* (Display/getHeight)
              *aspect-ratio* (/ (Display/getWidth) (Display/getHeight))
              *texture-cache* (preload-textures nil guy-texture fire-texture star-texture)
              *program-cache* (preload-programs nil identity-program no-texture-program)
              *screen-pbo* (gl-buffer)
              *screen-texture* (create-texture-id)
              *matrix-buffer* (create-float-buffer 16)
              *simplex-surface* (let [surface (simplex-surface 0.01 30 30 30)]
                                  {:buffer (gl-point-buffer 3 (:tris surface))
                                   :vertex-count (count (:tris surface))
                                   :normals (gl-point-buffer 3 (:norms surface))})]
      
      ;; allocate space for the screen pbo
      (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER *screen-pbo*)
      (GL15/glBufferData GL21/GL_PIXEL_PACK_BUFFER (* *width* *height* 4) GL15/GL_DYNAMIC_COPY)
      (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER 0)
      
      (init-gl)
      (game-loop))

    (finally (Display/destroy))))

