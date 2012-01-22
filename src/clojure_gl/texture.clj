(ns clojure-gl.texture
  (:use (clojure-gl buffers resources))
  (:import (java.awt Color Graphics)
           (java.awt.color ColorSpace)
           (java.awt.image ColorModel ComponentColorModel DataBuffer Raster BufferedImage)
           (javax.imageio ImageIO)
           (org.lwjgl.opengl GL11)
           (java.io BufferedInputStream)
           (java.nio ByteBuffer ByteOrder IntBuffer)
           (java.util Hashtable)))


(defn has-alpha [img]
  (.. img (getColorModel) (hasAlpha)))

(defn alpha-color-model []
  (ComponentColorModel. (ColorSpace/getInstance ColorSpace/CS_sRGB)
                        (int-array [8 8 8 8])
                        true
                        false
                        ComponentColorModel/TRANSLUCENT
                        DataBuffer/TYPE_BYTE))

(defn color-model []
  (ComponentColorModel. (ColorSpace/getInstance ColorSpace/CS_sRGB)
                        (int-array [8 8 8 0])
                        false
                        false
                        ComponentColorModel/OPAQUE
                        DataBuffer/TYPE_BYTE))

(defn convert-texture-data [img]
  (let [width (greater-power-of-two (.getWidth img))
        height (greater-power-of-two (.getHeight img))
        raster (if (has-alpha img)
                 (Raster/createInterleavedRaster DataBuffer/TYPE_BYTE width height 4 nil)
                 (Raster/createInterleavedRaster DataBuffer/TYPE_BYTE width height 3 nil))
        tex-image (if (has-alpha img)
                    (BufferedImage. (alpha-color-model) raster false (Hashtable.))
                    (BufferedImage. (color-model) raster false (Hashtable.)))
        tex-g (.getGraphics tex-image)]

    (doto tex-g
      (.setColor (Color. 0 0 0 0))
      (.fillRect 0 0 width height)
      (.drawImage img 0 0 nil))

    (let [data (.. tex-image (getRaster) (getDataBuffer) (getData))
          buffer (ByteBuffer/allocateDirect (count data))]
      (doto buffer
        (.order (ByteOrder/nativeOrder))
        (.put data 0 (count data))
        (.flip))
      buffer)))

(defn load-image [ref]
  (if-let [resource (resource-as-stream ref)]
    (ImageIO/read (BufferedInputStream. resource))))

(defn load-texture [resource]
  (if-let [image (load-image resource)]
    (let [texid (create-texture-id)
          width (greater-power-of-two (.getWidth image))
          height (greater-power-of-two (.getHeight image))
          bytes (convert-texture-data image)]
      (GL11/glBindTexture GL11/GL_TEXTURE_2D texid)
      (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR)
      (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR)
      (GL11/glTexImage2D GL11/GL_TEXTURE_2D
                         0
                         GL11/GL_RGBA
                         width height
                         0
                         (if (has-alpha image) GL11/GL_RGBA GL11/GL_RGB)
                         GL11/GL_UNSIGNED_BYTE
                         bytes)
      texid)))

(defn get-texture [resource texture-cache]
  (let [texture-cache (or texture-cache {})]
    (if-let [texture (texture-cache resource)]
      [texture texture-cache]
      (let [texture (load-texture resource)]
        [texture (conj texture-cache {resource texture})]))))

(defn preload-textures [cache & names]
  (loop [cache cache
         names names]
    (if (first names)
      (let [[_ cache] (get-texture (first names) cache)]
        (recur cache (rest names)))
      cache)))

(defn bind-texture [tex-id]
  (GL11/glBindTexture GL11/GL_TEXTURE_2D tex-id))