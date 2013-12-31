(ns fivetonine.collage.helpers
  (:import java.awt.image.BufferedImage))

(defn pixel-at
  [image x y]
  (-> image .getData (.getPixel x y (ints nil)) seq))

(defn buf-img
  ([w] (BufferedImage. w w BufferedImage/TYPE_INT_ARGB))
  ([w h] (BufferedImage. w h BufferedImage/TYPE_INT_ARGB))
  ([w h t] (BufferedImage. w h t)))
