(defproject fivetonine/collage "0.2.0"
  :description "Clean, minimal image processing library for Clojure"
  :url "https://github.com/karls/collage"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :plugins [[lein-marginalia "0.7.1"]]

  :profiles {:test {:resource-paths ["fixtures"]}}

  ;; :global-vars { *warn-on-reflection* true }

  ;; WebP support
  :resource-paths ["resources" "resources/webp-imageio.jar"]
  :java-source-paths ["src/fivetonine/collage/java"]

  ;; In order to actually use the WebP format, the JVM uses native code that
  ;; needs to be compiled by the user. The JVM loads the native code from the
  ;; native library path, which is set here. This may be overridden when
  ;; starting the JVM with -Djava.library.path=/your/custom/path/.
  ;; See the README for instructions on how to compile the native code.
  :jvm-opts [~(str "-Djava.library.path=native/:" (System/getenv "LD_LIBRARY_PATH"))])
