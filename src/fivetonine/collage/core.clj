;; ## Drop in library for (some of) your image processing needs.
;;
;; Collage was developed out of my own need to answer some specific needs in a
;; project I was working on. Even though there are a couple of other libraries
;; out there (mikera's [imagez](https://github.com/mikera/imagez), which builds
;; on [imgscalr](https://github.com/thebuzzmedia/imgscalr), a Java library),
;; but I felt like implementing my own in order to gain more experience with
;; Clojure.
;;
;; The feature-set is somewhat similar to the previously mentioned libraries,
;; adding functionality to paste layers (regular `BufferedImages`) onto an image
;; and controlling the quality of the image when saving it to disk.
;;
;; ### This project aims to
;; * Be an easy to use, drop-in solution
;; * Have a composable internal API
;; * Be well tested
;; * Be reasonably idiomatic
;;
(ns fivetonine.collage.core
  (:require [fivetonine.collage.util :as util])
  (:import java.awt.color.ColorSpace)
  (:import java.awt.image.BufferedImage)
  (:import java.awt.image.ColorConvertOp)
  (:import java.awt.image.Raster)
  (:import java.awt.geom.AffineTransform)
  (:import java.awt.AlphaComposite)
  (:import java.awt.Color)
  (:import java.awt.Polygon)
  (:import java.awt.RenderingHints))

(declare resize*)
(declare paste*)
(declare normalise-angle)
(declare pi-rotation?)

(def not-nil? (complement nil?))

;; ## Core functions

(defn rotate
  "Rotates image through angle `theta`, where `theta` is
  an integer multiple of 90.

  If `theta > 0`, the image is rotated clockwise.

  If `theta < 0`, the image is rotated anticlockwise."
  [image theta]
  (when-not (contains? (set (range -360 450 90)) (normalise-angle theta))
    (throw (IllegalArgumentException.
            "theta has to be an integer multiple of 90.")))
  (let [old-width (.getWidth image)
        old-height (.getHeight image)
        new-width (if (pi-rotation? theta) old-width old-height)
        new-height (if (pi-rotation? theta) old-height old-width)
        angle (Math/toRadians theta)
        new-image (BufferedImage. new-width new-height (.getType image))
        graphics (.createGraphics new-image)
        transform (AffineTransform.)]

    ;; Given that the rotation happens around the point (0,0) (the top left hand
    ;; corner of the image), the resulting image needs to be translated back
    ;; into the "viewport".
    (condp = (Math/abs (normalise-angle theta))
      0   (.translate transform 0 0)
      90  (.translate transform new-width 0)
      180 (.translate transform new-width new-height)
      270 (.translate transform 0 new-height)
      360 (.translate transform 0 0))
    (.rotate transform angle)

    (doto graphics
      (.drawImage image transform nil)
      (.dispose))
    new-image))

(defn flip
  "Flips an image.

  If direction is `:horizontal`, flips the image around the y-axis.

  If direction is `:vertical`, flips the image around the x-axis."
  [image direction]
  (let [width (.getWidth image)
        height (.getHeight image)
        new-image (BufferedImage. width height (.getType image))
        graphics (.createGraphics new-image)
        transform (AffineTransform.)]
    (case direction
      :horizontal (doto transform
                    (.translate width 0)
                    (.scale -1 1))
      :vertical (doto transform
                  (.translate 0 height)
                  (.scale 1 -1)))

    (doto graphics
      (.drawImage image transform nil)
      (.dispose))

    new-image))

(defn scale
  "Scales an image by a factor `f`.

  If `0.0 < f < 1.0` the image is scaled down.

  If `f > 1.0` the image is scaled up."
  [image f]
  (resize* image
           (* f (-> image .getWidth int))
           (* f (-> image .getHeight int))))

(defn crop
  "Crops an image.

  `x, y` are the coordinates to top left corner of the area to crop.
  `width, height` are the width and height of the area to crop.

  The returned image does not share its data with the original image."
  [image x y width height]
  (-> image (.getSubimage x y width height) util/copy))

(defn resize
  "Resizes an image.

  If only `width` or `height` is provided, the resulting image will be `width`
  or `height` px wide, respectively. The other dimension will be calculated
  automatically to preserve `width/height` ratio.

  With `width` and `height` both provided, the resulting image will be crudely
  resized to match the provided values.

  If neither `width` nor `height` are provided, `IllegalArgumentException` is
  thrown.

  Examples:

    (resize image :width 100)
    (resize image :height 300)
    (resize image :width 100 :height 300)"
  [image & {:keys [width height] :as opts}]
  (let [supported #{:width :height}
        options (select-keys opts supported)
        width (options :width)
        height (options :height)]
    (when (empty? options)
      (throw (IllegalArgumentException.
              "Width or height (or both) has to be provided.")))
    (cond
     (and width height) (resize* image (int width) (int height))

     (not-nil? width)
     (let [new-height (* (/ (int width) (.getWidth image)) (.getHeight image))]
       (resize* image width (int new-height)))

     (not-nil? height)
     (let [new-width (* (/ (int height) (.getHeight image)) (.getWidth image))]
       (resize* image (int new-width) height)))))

(defn resize*
  "Resize the given image to `width` and `height`.
  Used as an internal function by `resize`.

  Note: the method of resizing may change in the future as there are better,
  iterative, solutions to balancing speed vs. quality. See
  [the perils of Image.getScaledInstance()](https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html)."
  [image width height]
  (let [new-image (BufferedImage. width height (.getType image))
        graphics (.createGraphics new-image)]
    (doto graphics
      (.setRenderingHint RenderingHints/KEY_INTERPOLATION
                         RenderingHints/VALUE_INTERPOLATION_BICUBIC)
      (.drawImage image 0 0 width height nil)
      .dispose)
    new-image))

(defn paste
  "Pastes layer(s) onto image at coordinates `x` and `y`.

  `layer-defs` is expected to in the format

    [layer1 x1 y2 layer2 x2 y2 ... ]

  Layers are loaded using `fivetonine.collage.util/load-image`.
  Top left corner of a layer will be at `x, y`.

  Throws `IllegalArgumentException` if the number of elements in the list of
  layers and coordinates is not divisible by 3.

  Returns the resulting image."
  [image & layer-defs]
  (let [args (flatten (seq layer-defs))]
    (when-not (zero? (-> args count (rem 3)))
      (throw (IllegalArgumentException.
              "Expected layer-defs format [image1 x1 y1 image2 x2 y2 ... ].")))
    (let [new-image (util/copy image)
          layers (partition 3 args)]
      ;; "reduce" all the layers onto new-image using paste* -- the layers are
      ;; accumulated onto the image
      (reduce paste* new-image layers)
      new-image)))

(defn paste*
  "Paste layer on top of base at position `x, y` and return the resulting image.
  Used as an internal function by `paste`."
  [base [layer x y]]
  (let [graphics (.createGraphics base)]
    (doto graphics
      (.setRenderingHint RenderingHints/KEY_RENDERING
                         RenderingHints/VALUE_RENDER_QUALITY)
      (.setRenderingHint RenderingHints/KEY_COLOR_RENDERING
                         RenderingHints/VALUE_COLOR_RENDER_QUALITY)
      (.drawImage (util/load-image layer) x y nil)
      (.dispose))
    base))

(defn circle
  "Crop a circle from an image, leaving a transparent background. This function
  does not compose well with other functions that cut from the image, as its
  implementation doesn't really cut - it paints parts of the image onto a
  circle. If you need to compose circles with other shapes, create the circle
  first.

  Why must this function be so limited? Using a clip path yields a much too
  jagged circle. Instead we paint a beautifully anti-aliased circle, and then
  composit the image on top. This yields a pleasantly anti-aliased circle image
  with the drawback that if you apply the transformation to a transparent image,
  you will see the parts of the white circle the image doesn't cover."
  [^BufferedImage image]
  (let [width (.getWidth image)
        circle (BufferedImage. width width BufferedImage/TYPE_INT_ARGB)]
    (doto (.createGraphics circle)
      (.setComposite AlphaComposite/Clear)
      (.fillRect 0 0 width width)
      ;; Fake soft-clipping by first drawing the desired clip shape in fully
      ;; opaque white antialiasing enabled
      (.setComposite AlphaComposite/Src)
      (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
      (.setColor (Color/WHITE))
      (.fill (java.awt.geom.Ellipse2D$Float. 0 0 width width))
      ;; Then composit the image on top
      (.setComposite AlphaComposite/SrcAtop)
      (.drawImage image 0 0 nil)
      .dispose)
    circle))

(defn- triangle-polygon [pos ^long w]
  (case pos
    :lower-left (Polygon. (int-array [0 w 0]) (int-array [w w 0]) 3)
    :lower-right (Polygon. (int-array [w w 0]) (int-array [w 0 w]) 3)
    :upper-right (Polygon. (int-array [w 0 w]) (int-array [0 0 w]) 3)
    :upper-left (Polygon. (int-array [0 0 w]) (int-array [0 w 0]) 3)))

(defn triangle
  "Crop a triangle from an image, leaving a transparent background. Can optionally
  be called with an option map that specifies where the triangle is positioned:

  ```clj
  (triangle image {:position :upper-left})
  (triangle image {:position :lower-left})
  (triangle image {:position :lower-right})
  (triangle image {:position :upper-right})
  ```"
  [^BufferedImage image & [{:keys [position]}]]
  (let [width (.getWidth image)
        triangle (BufferedImage. width width BufferedImage/TYPE_INT_ARGB)]
    (doto (.createGraphics triangle)
      (.setComposite AlphaComposite/Clear)
      (.fillRect 0 0 width width)
      (.setComposite AlphaComposite/Src)
      (.setClip (triangle-polygon (or position :upper-left) width))
      (.drawImage image 0 0 width width nil)
      .dispose)
    triangle))

(defn grayscale
  "Convert an image to grayscale."
  [^BufferedImage image]
  (let [output (BufferedImage. (.getWidth image) (.getHeight image) (.getType image))]
    (-> (ColorConvertOp. (.. image getColorModel getColorSpace) (ColorSpace/getInstance ColorSpace/CS_GRAY) nil)
        (.filter image output))
    output))

(defn- align-bands
  "Align the number of bands in the color with the specified bands - basically,
  ensure that the color has an alpha channel if there are 4 bands, and that it
  doesn't if there are 3. If an alpha channel must be added, it is set to fully
  opaque - 255."
  [bands color]
  (let [c (count color)]
    (cond
      (< bands c) (take bands color)
      (< c bands) (concat color [255])
      :default color)))

(defn duotone-filter
  "Actually apply the duotone filter to the raster. This function uses a tight
  loop over the pixels in the image, and is _very_ performance sensitive. If
  attempting to refactor/simplify this function, benchmark your efforts before
  and after, as it is very easy to trigger reflection and/or boxing, which
  causes abysmal performance."
  [^Raster raster from to]
  (let [dest (.createCompatibleWritableRaster raster)
        bands (.getNumBands dest)
        dest-pixel (int-array bands)
        w (.getWidth raster)
        h (.getHeight raster)
        from (int-array (align-bands bands from))
        diff (int-array (map #(- %1 %2) (align-bands bands to) from))]
    (loop [y 0]
      (when (< y h)
        (loop [x 0
               pixel (int-array (repeat bands 0))]
          (when (< x w)
            (let [pixel (.getPixel raster x y pixel)
                  factor (/ (aget pixel 0) 255.0)]
              (dotimes [n bands]
                (aset dest-pixel n (int (+ (aget from n) (* (aget diff n) factor)))))
              (.setPixel dest x y dest-pixel)
              (recur (inc x) pixel))))
        (recur (inc y))))
    dest))

(defn duotone
  "Create a duotone image. A duotone image is like a grayscale image, but instead
  of mapping each pixel to the range black/white, it maps the pixels along the
  gradient of an arbitrary from color to an arbitrary to color - e.g. from red
  to blue. See samples at https://duotones.co

  `from` and `to` are colors as vectors of R G B, and optionally Alpha values in
  the range 0-255. The alpha channel can be omitted, in which case it will be
  set to 255 if required by the image (e.g., if it is an ARGB image)."
  [^BufferedImage image from to]
  (let [color-model (.getColorModel image)
        raster (duotone-filter (.getRaster (grayscale image)) from to)]
    (BufferedImage. color-model raster (.isAlphaPremultiplied color-model) nil)))

(defmacro with-image
  "A helper for applying multiple operations to an image.

  `image-resource` can be a `String`, a `File` or a `BufferedImage`.

  Example:

    (with-image \"/path/to/image.jpg\"
                 (scale 0.8)
                 (rotate 90)
                 (crop 0 0 100 100))

  Expands to (properly namespaced):

    (let [image__2336__auto__ (load-image \"/path/to/image.jpg\")]
      (clojure.core/-> image__2336__auto__
                       (scale 0.8)
                       (rotate 90)
                       (crop 0 0 100 100)))

  Returns the image which is the result of applying all operations to the input
  image."
  [image-resource & operations]
  `(let [image# (util/load-image ~image-resource)]
     (-> image# ~@operations)))

;; ## Helpers

(defn- pi-rotation?
  "Does the rotation through angle `theta` correspond to a rotation that is
  a multiple of 180 degrees, meaning that the image preserves the original
  width and height."
  [theta]
  (-> theta (/ 90) (rem 2) (= 0)))

(defn normalise-angle
  "Restrict the rotation angle to the range [-360..360]."
  [theta]
  (rem theta 360))
