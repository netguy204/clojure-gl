(ns clojure-gl.buffers
  (:import (java.nio ByteBuffer ByteOrder IntBuffer FloatBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.opengl GL11 GL15 GL20 GL30)))

(defn native-byte-buffer [sz]
  (let [bb (ByteBuffer/allocateDirect (* 4 sz))]
    (.order bb (ByteOrder/nativeOrder))
    bb))

(defn create-int-buffer [sz]
  (.asIntBuffer (native-byte-buffer sz)))

(defn create-float-buffer [sz]
  (BufferUtils/createFloatBuffer sz))

(defn create-texture-id []
  (let [^IntBuffer ib (create-int-buffer 1)]
    (GL11/glGenTextures ib)
    (.get ib 0)))

(defn gl-buffer []
  (let [^IntBuffer ib (create-int-buffer 1)]
    (GL15/glGenBuffers ib)
    (.get ib 0)))

(defn point-float-buffer [arity array-of-verts]
  (let [sz (* arity (count array-of-verts))
        ^FloatBuffer fb (create-float-buffer sz)]
    (doseq [vert array-of-verts]
      (.put fb (float-array vert)))
    (.flip fb)
    fb))

(defn gl-point-buffer [arity array-of-verts]
  (let [glb (gl-buffer)
        ^FloatBuffer fb (point-float-buffer arity array-of-verts)]
    (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER glb)
    (GL15/glBufferData GL15/GL_ARRAY_BUFFER fb GL15/GL_STATIC_DRAW)
    glb))

(defn greater-power-of-two [n]
  (loop [x 2]
    (if (< x n)
      (recur (* x 2))
      x)))

(defn gl-bind-buffer [buffer ^Integer arity ^Integer attribute]
  (GL20/glEnableVertexAttribArray attribute)
  (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER buffer)
  (GL20/glVertexAttribPointer attribute arity GL11/GL_FLOAT false (int 0) (long 0)))

