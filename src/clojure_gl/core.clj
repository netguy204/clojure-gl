(ns clojure-gl.core
  (:use (clojure-gl prepare texture))
  (:import (org.lwjgl LWJGLException)
           (org.lwjgl.opengl Display GL11)))

(defn start-thread [runnable]
  (.start (Thread. runnable)))

(defonce exit-requested (ref false))

(def ^:dynamic *width* nil)
(def ^:dynamic *height* nil)
(def ^:dynamic *aspect-ratio* nil)
(def ^:dynamic *texture-cache* nil)
(def guy-texture "clojure-gl/guy.png")

(defn draw-tri [p1 p2 p3 t1 t2 t3]
  (GL11/glBindTexture GL11/GL_TEXTURE_2D (*texture-cache* guy-texture))
  (GL11/glBegin GL11/GL_TRIANGLES)
  (GL11/glTexCoord2f (t1 0) (t1 1))
  (GL11/glVertex2f (p1 0) (p1 1))
  (GL11/glTexCoord2f (t2 0) (t2 1))
  (GL11/glVertex2f (p2 0) (p2 1))
  (GL11/glTexCoord2f (t3 0) (t3 1))
  (GL11/glVertex2f (p3 0) (p3 1))
  (GL11/glEnd)

  (GL11/glBegin GL11/GL_LINE_LOOP)
  (GL11/glVertex2f (p1 0) (p1 1))
  (GL11/glVertex2f (p2 0) (p2 1))
  (GL11/glVertex2f (p3 0) (p3 1))
  (GL11/glEnd))

(defn game-state-init []
  0)

(defn game-cycle [dtms game-state]
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (GL11/glColor3f 1.0 1.0 1.0)
  (GL11/glPushMatrix)
  (GL11/glTranslatef (* 0.5 *aspect-ratio*) 0.5 0.0)
  (GL11/glScalef 0.5 0.5 0.5)
  (GL11/glRotatef (mod (/ game-state 100) 360) 0.0 0.0 1.0)
  (draw-tri [0.0 0.5] [-0.5 -0.5] [0.5 -0.5]
            [0.4 0.7] [0.0 0.0] [0.8 0.0])
  (GL11/glPopMatrix)
  (Display/update)
  (Display/sync 60)
  (+ game-state dtms))

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
  (GL11/glDisable GL11/GL_DEPTH_TEST))

(defn run []
  (Display/setTitle "Hello World")
  (Display/create)

  (try
    (binding [*width* (Display/getWidth)
              *height* (Display/getHeight)
              *aspect-ratio* (/ (Display/getWidth) (Display/getHeight))
              *texture-cache* (preload-textures nil guy-texture)]
      (init-gl)
      (game-loop))

    (finally (Display/destroy))))

