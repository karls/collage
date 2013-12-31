(ns fivetonine.collage.core-test
  (:require [clojure.test :refer :all]
            [fivetonine.collage.core :refer :all]
            [fivetonine.collage.helpers :refer :all])
  (:import java.awt.image.BufferedImage
           java.awt.Color))

(deftest scale-test
  (let [image (buf-img 100)]
    (testing "scale factor < 1.0"
      (let [scaled (scale image 0.6)]
        (is (= 60 (.getWidth scaled)))
        (is (= 60 (.getHeight scaled)))))

    (testing "scale factor > 1.0"
      (let [scaled (scale image 1.5)]
        (is (= 150 (.getWidth scaled)))
        (is (= 150 (.getHeight scaled)))))

    (testing "scale factor = 1.0"
      (let [scaled (scale image 1.0)]
        (is (= 100 (.getWidth scaled)))
        (is (= 100 (.getHeight scaled)))))))

(deftest crop-test
  (let [image (buf-img 100)]
    (is (= 50 (.getWidth  (crop image 0 0 50 10))))
    (is (= 10 (.getHeight (crop image 0 0 50 10))))
    (is (= 15 (.getWidth  (crop image 50 90 15 5))))
    (is (= 5  (.getHeight (crop image 50 90 15 5))))
    ;; going out of bounds
    (is (thrown? Exception (crop image 90 0 11 10)))
    (is (thrown? Exception (crop image 0 90 10 11)))))

(deftest resize-test
  (let [image (buf-img 800 600)]
    (testing "no width and height provided"
      (is (thrown? IllegalArgumentException (resize image))))

    (testing "with width provided"
      (let [resized (resize image :width 400)]
        (is (= 400 (.getWidth resized)))
        (is (= 300 (.getHeight resized)))))

    (testing "with height provided"
      (let [resized (resize image :height 300)]
        (is (= 400 (.getWidth resized)))
        (is (= 300 (.getHeight resized)))))

    (testing "with width and height both provided"
      (let [resized (resize image :width 50 :height 30)]
        (is (= (.getWidth resized) 50))
        (is (= (.getHeight resized) 30))))))

(deftest resize*-test
  (let [image (buf-img 100)
        resized (resize image :width 50 :height 30)]
    (is (= (.getWidth resized) 50))
    (is (= (.getHeight resized) 30))))

(deftest paste-test
  (let [image (buf-img 10 10)]
    (testing "wrong number of layer args"
      ;; the values of arguments don't matter in the layer vector, as they're
      ;; checked only when it's certain that there is a correct number of them
      (is (thrown? IllegalArgumentException (paste image [:foo :bar])))
      (is (thrown? IllegalArgumentException (paste image [:foo :bar :baz :qux]))))

    (testing "no layers"
      (is (instance? BufferedImage (paste image [])))
      (is (not (= image (paste image [])))))

    (testing "some layers"
      (let [layer1 (buf-img 2)
            graphics1 (.createGraphics layer1)
            layer2 (buf-img 2)
            graphics2 (.createGraphics layer2)]

        (is (instance? BufferedImage (paste image [layer1 0 0])))
        (is (instance? BufferedImage (paste image [layer1 0 0 layer2 5 5])))
        (is (not (= image (paste image [layer1 0 0 layer2 5 5]))))

        (doto graphics1
          (.setPaint (Color/yellow))
          (.fillRect 0 0 2 2)
          (.dispose))
        (doto graphics2
          (.setPaint (Color/green))
          (.fillRect 0 0 2 2)
          (.dispose))

        (let [pasted (paste image [layer1 0 0 layer2 9 9])]
          (is (= (pixel-at pasted 9 0) (pixel-at pasted 0 9)))
          (is (not (= (pixel-at pasted 0 0) (pixel-at pasted 9 9))))
          (is (not (= (pixel-at pasted 0 0) (pixel-at pasted 0 9))))
          (is (not (= (pixel-at pasted 0 9) (pixel-at pasted 9 9)))))))))

(deftest paste*-test
  (let [image (buf-img 10)
        layer (buf-img 5)
        layer-graphics (.createGraphics layer)]
    (doto layer-graphics
      (.setPaint (Color/yellow))
      (.fillRect 0 0 5 5)
      (.dispose))
    (let [pasted (paste* image [layer 0 0])]
      (testing "applying a layer"
        (is (= (pixel-at pasted 0 0) (pixel-at pasted 4 0)))
        (is (not (= (pixel-at pasted 0 0) (pixel-at pasted 5 0))))))))

(deftest rotate-test
  (let [dimensions [640 480]
        image (buf-img (dimensions 0) (dimensions 1))
        new-dims #(let [r (rotate image %1)] [(.getWidth r) (.getHeight r)])]
    (are [theta dim] (= dim (new-dims theta))
         0    dimensions
         90   (reverse dimensions)
         180  dimensions
         270  (reverse dimensions)
         360  dimensions
         450  (reverse dimensions)
         -90  (reverse dimensions)
         -180 dimensions
         -270 (reverse dimensions)
         -360 dimensions
         -450 (reverse dimensions))))
