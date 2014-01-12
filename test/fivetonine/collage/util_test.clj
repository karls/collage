(ns fivetonine.collage.util-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [as-file resource]]
            [clojure.string :as str]
            [fivetonine.collage.util :refer :all]
            [fivetonine.collage.helpers :refer :all])
  (:import java.io.File
           java.awt.image.BufferedImage
           javax.imageio.ImageWriteParam
           java.awt.Color
           javax.imageio.ImageIO
           javax.swing.JFrame
           java.net.URI))

(def test-image-paths
  {"fixtures/images/cloud.png" "fixtures/images/cloud-new.png"
   "fixtures/images/apartment.jpg" "fixtures/images/apartment-new.jpg"})

(defn cleanup-images []
  (doseq [path (vals test-image-paths)]
    (let [file (File. path)]
      (when (.isFile file)
        (.delete file)))))

(deftest load-image-test
  (let [path (first (keys test-image-paths))]
    (are [image] (instance? BufferedImage image)
         (load-image path)
         (load-image (as-file path))
         (load-image (ImageIO/read (as-file path)))
         (load-image (resource (str/replace path #"^fixtures/" ""))))))

(deftest sanitize-path-test
  (is (instance? URI (sanitize-path "file:///path/to/some/image.png")))
  (is (instance? URI (sanitize-path (first (keys test-image-paths)))))
  (is (thrown? Exception (sanitize-path "http://www.foobar.com/image.png"))))

(deftest save-test
  (testing "default options"
    (doseq [[path new-path] (seq test-image-paths)]
      (let [image (load-image path)
            saved-image (save image new-path)]
        (is (.exists (File. saved-image)))))
    (cleanup-images))

  (testing "with progressive mode"
    (testing "turned on"
      (doseq [[path new-path] (seq test-image-paths)]
        (let [image (load-image path)
              saved-image #(save image new-path :progressive true)]
          (is (.exists (File. (saved-image))))))
      (cleanup-images))
    (testing "turned off"
      (doseq [[path new-path] (seq test-image-paths)]
        (let [image (load-image path)
              saved-image #(save image new-path :progressive false)]
          (is (.exists (File. (saved-image)))))))
    (cleanup-images))
  (testing "with explicit quality coefficient"))

(deftest copy-test
  (testing "creating a new object"
    (let [image (BufferedImage. 1 1 BufferedImage/TYPE_INT_ARGB)]
      (is (not (= image (copy image))))))

  (testing "changing old image doesn't change new image"
    (let [image1 (BufferedImage. 1 1 BufferedImage/TYPE_INT_RGB)
          graphics1 (.createGraphics image1)
          image2 (copy image1)
          graphics2 (.createGraphics image2)]
      (doto graphics1
        (.setPaint (Color/yellow))
        (.fillRect 0 0 1 1)
        (.dispose))
      (is (not (= (pixel-at image1 0 0) (pixel-at image2 0 0)))))))
