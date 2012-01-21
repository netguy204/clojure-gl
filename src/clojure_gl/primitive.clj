(ns clojure-gl.primitive
  (:use (clojure-gl texture))
  (:import (org.lwjgl.opengl GL11)))

(defn draw-unit-quad []
  (GL11/glBegin GL11/GL_QUADS)
  (GL11/glTexCoord2f 0 0)
  (GL11/glVertex2f -0.5 -0.5)
  (GL11/glTexCoord2f 0 1)
  (GL11/glVertex2f -0.5 0.5)
  (GL11/glTexCoord2f 1 1)
  (GL11/glVertex2f 0.5 0.5)
  (GL11/glTexCoord2f 1 0)
  (GL11/glVertex2f 0.5 -0.5)
  (GL11/glEnd))

(defn draw-tri [p1 p2 p3 t1 t2 t3 texture-id]
  (bind-texture texture-id)
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

