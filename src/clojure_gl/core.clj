(ns clojure-gl.core
  (:use (clojure-gl prepare texture primitive particle))
  (:import (org.lwjgl LWJGLException)
           (org.lwjgl.opengl Display GL11 GL14)))

(defn start-thread [runnable]
  (.start (Thread. runnable)))

(def ^:dynamic *width* nil)
(def ^:dynamic *height* nil)
(def ^:dynamic *aspect-ratio* nil)
(def ^:dynamic *texture-cache* nil)

(def guy-texture "clojure-gl/guy.png")
(def fire-texture "clojure-gl/fire.png")
(def star-texture "clojure-gl/star.png")

(def num-particles 150)

(defn game-state-init []
  {:time 0
   :fires (for [x (range num-particles)] (fire-particle [0 0]))})

(defn game-cycle [dtms game-state]
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (GL11/glColor3f 1.0 1.0 1.0)
  (GL11/glPushMatrix)
  (GL11/glTranslatef (* 0.5 *aspect-ratio*) 0.5 0.0)
  (GL11/glScalef 0.25 0.25 0.25)
  
  (bind-texture (*texture-cache* fire-texture))
  (doseq [particle (game-state :fires)]
    (GL11/glColor4f 1.0 1.0 1.0 (exp-alpha particle))
    (GL11/glPushMatrix)
    (GL11/glTranslatef ((particle :center) 0) ((particle :center) 1) 0.0)
    (GL11/glScalef (particle :scale) (particle :scale) (particle :scale))
    (GL11/glRotatef (particle :rotation) 0.0 0.0 1.0)
    (draw-unit-quad)
    (GL11/glPopMatrix))
  
  (GL11/glPopMatrix)
  (Display/update)
  (Display/sync 30)

  (conj game-state
        {:time (+ (game-state :time) dtms)
         :fires (for [particle (game-state :fires)]
                  (if-let [particle (update-particle particle (/ dtms 1000))]
                    particle
                    (fire-particle [0 0])))}))

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

(defn run []
  (Display/setTitle "Hello World")
  (Display/create)

  (try
    (binding [*width* (Display/getWidth)
              *height* (Display/getHeight)
              *aspect-ratio* (/ (Display/getWidth) (Display/getHeight))
              *texture-cache* (preload-textures nil guy-texture fire-texture star-texture)]
      (init-gl)
      (game-loop))

    (finally (Display/destroy))))

