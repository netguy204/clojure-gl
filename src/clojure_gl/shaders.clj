(ns clojure-gl.shaders
  (:use (clojure-gl resources))
  (:import (org.lwjgl.opengl GL11 GL20)))

(def ^:dynamic *attribute-vertex* 0)
(def ^:dynamic *attribute-texture-coords* 1)

(defn load-shader [res kind]
  (if-let [lines (resource-as-string res)]
    (let [shader (GL20/glCreateShader kind)]
      (GL20/glShaderSource shader lines)
      (GL20/glCompileShader shader)
      (if (== (GL20/glGetShader shader GL20/GL_COMPILE_STATUS) GL11/GL_FALSE)
        (let [error (GL20/glGetShaderInfoLog shader 2048)]
          (GL20/glDeleteShader shader)
          (throw (RuntimeException. error)))
        shader))))

(defn link-program [shaders attributes]
  (let [program (GL20/glCreateProgram)]
    (doseq [shader shaders]
      (GL20/glAttachShader program shader))
    (doseq [attrib attributes]
      (let [[id name] attrib]
        (GL20/glBindAttribLocation program id name)))
    (GL20/glLinkProgram program)
    (if (== (GL20/glGetProgram program GL20/GL_LINK_STATUS) GL11/GL_FALSE)
      (let [error (GL20/glGetProgramInfoLog program 2048)]
        (GL20/glDeleteProgram program)
        (throw (RuntimeException. error)))
      program)))

(defn- free-shaders [shaders]
  (doseq [shader shaders]
    (GL20/glDeleteShader shader)))

(defn load-program [program-resource]
  (let [shader-names (program-resource :shaders)
        attributes (program-resource :attributes)
        shaders []
        shaders (if (shader-names :vertex)
                  (conj shaders (load-shader (shader-names :vertex) GL20/GL_VERTEX_SHADER))
                  shaders)
        shaders (if (shader-names :fragment)
                  (conj shaders (load-shader (shader-names :fragment) GL20/GL_FRAGMENT_SHADER))
                  shaders)]
    (try
      (link-program shaders attributes)
      (finally
        (free-shaders shaders)))))

(defn get-program [program-resource program-cache]
  (let [program-cache (or program-cache {})
        name (program-resource :name)]
    (if-let [program (program-cache name)]
      [program program-cache]
      (let [program (load-program program-resource)]
        [program (conj program-cache {name program})]))))

(defn preload-programs [cache & resources]
  (loop [cache cache
         resources resources]
    (if (first resources)
      (let [[_ cache] (get-program (first resources) cache)]
        (recur cache (rest resources)))
      cache))  )