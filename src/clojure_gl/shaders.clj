(ns clojure-gl.shaders
  (:use (clojure-gl resources buffers shader-variables))
  (:import (org.lwjgl.opengl GL11 GL20)))

(def ^:dynamic *attribute-vertex* 0)
(def ^:dynamic *attribute-texture-coords* 1)
(def ^:dynamic *attribute-texture0* 2)

(defn- as-map [seq]
  (into {} seq))

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

(defn- assign-attribute-numbers [attributes]
  (zipmap (keys attributes)
          (range (count attributes))))

(defn- attribute-bindings [attributes]
  (let [numbers (assign-attribute-numbers attributes)
        number-to-name (as-map (for [key (keys attributes)]
                                 [(numbers key) (:name (attributes key))]))
        arities (as-map (for [key (keys attributes)]
                          [key (:arity (attributes key))]))]
    {:attribute-number-mapping numbers
     :attribute-arity-mapping arities
     :number-to-name number-to-name}))

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

(defn- internal-get-uniform-location [program ^String name]
  (let [^Integer program-number (:program-number program)]
    (GL20/glGetUniformLocation program-number name)))

(defn- with-uniform-mapping [program]
  (conj program
        {:uniform-mapping
         (as-map (for [[symbol name] (:uniforms program)]
                   [symbol (internal-get-uniform-location program name)]))}))

(defn get-uniform-location [program symbol]
  ((:uniform-mapping program) symbol))

(defn get-attribute-location [program symbol]
  ((:attribute-number-mapping program) symbol))

(defn get-attribute-arity [program symbol]
  ((:attribute-arity-mapping program) symbol))

(defn bind-program-attribute [program attribute buffer]
  (gl-bind-buffer buffer
                  (get-attribute-arity program attribute)
                  (get-attribute-location program attribute)))

(defn bind-program-uniform [program uniform value]
  (uniform-bind value (get-uniform-location program uniform)))

(defn load-program [program-resource]
  (let [shader-names (program-resource :shaders)
        shaders []
        shaders (if (shader-names :vertex)
                  (conj shaders (load-shader (shader-names :vertex) GL20/GL_VERTEX_SHADER))
                  shaders)
        shaders (if (shader-names :fragment)
                  (conj shaders (load-shader (shader-names :fragment) GL20/GL_FRAGMENT_SHADER))
                  shaders)]
    (try
      (link-program shaders (:number-to-name program-resource))
      (finally
        (free-shaders shaders)))))

(defn get-program [program-resource program-cache]
  (let [program-cache (or program-cache {})
        name (program-resource :name)]
    (if-let [program-record (program-cache name)]
      [program-record program-cache]
      (let [attributes (:attributes program-resource)
            program-resource (conj program-resource (attribute-bindings attributes))
            program-number (load-program program-resource)
            program-record (with-uniform-mapping
                             (conj program-resource {:program-number program-number}))]
        [program-record (conj program-cache {name program-record})]))))

(defn use-program [program]
  (let [^Integer program-number (:program-number program)]
   (GL20/glUseProgram program-number)))

(defn preload-programs [cache & resources]
  (loop [cache cache
         resources resources]
    (if (first resources)
      (let [[_ cache] (get-program (first resources) cache)]
        (recur cache (rest resources)))
      cache)))

(defn progify [program cache]
  (let [program-number (get-program program cache)]
    program-number))

