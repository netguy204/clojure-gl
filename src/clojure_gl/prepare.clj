(ns clojure-gl.prepare
  (:import (com.nullprogram.guide Arch NativeGuide)))

(defn- ng [arch lib]
  (NativeGuide/prepare arch lib))

(let [l64 (partial ng Arch/LINUX_64)
      l32 (partial ng Arch/LINUX_32)
      m64 (partial ng Arch/MAC_64)
      w64 (partial ng Arch/WINDOWS_64)
      w32 (partial ng Arch/WINDOWS_32)]
  (l64 "/liblwjgl64.so")
  (l32 "/liblwjgl.so")
  (m64 "/liblwjgl.jnilib"))
