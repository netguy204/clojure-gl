(ns clojure-gl.core
  (:use (clojure-gl prepare))
  (:import (org.lwjgl LWJGLException)
           (org.lwjgl.opengl Display GL11)))

(defn start-thread [runnable]
  (.start (Thread. runnable)))

(defonce exit-requested (ref false))

(def ^:dynamic *width* nil)
(def ^:dynamic *height* nil)
(def ^:dynamic *aspect-ratio* nil)

(defn start-exit-checker []
  (dosync (ref-set exit-requested false))
  (start-thread
   (fn []
     (print "starting check thread")
     (while (not (Display/isCloseRequested))
       (Thread/sleep 100))
     (print "leaving check thread")
     (dosync (ref-set exit-requested true)))))

(defn draw-tri [p1 p2 p3]
  (GL11/glBegin GL11/GL_TRIANGLES)
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
  (draw-tri [0.0 0.5] [-0.5 -0.5] [0.5 -0.5])
  (GL11/glPopMatrix)
  (Display/update)
  (Display/sync 60)
  (+ game-state dtms))

(defn game-loop []
  (loop [last-time (System/currentTimeMillis)
         game-state (game-state-init)]
    (let [current-time (System/currentTimeMillis)
          next-game-state (#'game-cycle (- current-time last-time) game-state)]
      (Thread/sleep 20)
      (if-not @exit-requested
        (recur current-time next-game-state)))))

(defn init-gl []
  (GL11/glMatrixMode GL11/GL_PROJECTION)
  (GL11/glLoadIdentity)
  (GL11/glOrtho 0 *aspect-ratio* 0 1 1 -1)
  (GL11/glMatrixMode GL11/GL_MODELVIEW))

(defn run []
  (Display/setTitle "Hello World")
  (Display/create)

  (start-exit-checker)

  (try
    (binding [*width* (Display/getWidth)
              *height* (Display/getHeight)
              *aspect-ratio* (/ (Display/getWidth) (Display/getHeight))]
      (init-gl)
      (game-loop))

    (finally (Display/destroy))))

