(ns designvote.media.core
  "Helpful functions to deal with images and other media"
  (:require [clojure.java.io :as io])
  (:import (javax.imageio ImageIO)
           (java.awt.image BufferedImage AffineTransformOp)
           (java.awt Graphics2D)
           (java.awt.geom AffineTransform)))

(defn- get-img-ratio
  "Compute img ratio for resizing"
  [{:keys [width height max-width max-height img-width img-height]}]
  (cond
    (and (some? height) (some? width))
    (min (/ width img-width) (/ height img-height))

    (some? height)
    (/ height img-height)

    (some? width)
    (/ width img-width)

    (and (some? max-height) (some? max-width))
    (min 1 (/ img-width max-width) (/ img-height max-height))

    (some? max-height)
    (min 1 (/ img-height max-height))

    (some? max-width)
    (min 1 (/ img-width max-width))

    :else
    1))


(defn resize-image
  "Resize image based on fixed or maximum achievable width/height"
  [^BufferedImage img {:keys [width height
                              max-width max-height]}]
  (let [img-width (.getWidth img)
        img-height (.getHeight img)
        ratio (get-img-ratio {:width     width :height height
                              :max-width max-width :max-height max-height
                              :img-width img-width :img-height img-height})
        ^BufferedImage img-scaled
        (if (= 1 ratio)
          img
          (let [scale
                (AffineTransform/getScaleInstance
                  (double ratio) (double ratio))

                transform-op
                (AffineTransformOp.
                  scale AffineTransformOp/TYPE_BILINEAR)]
            (.filter transform-op
                     img
                     (BufferedImage. (* ratio img-width)
                                     (* ratio img-height)
                                     (.getType img)))))]
    img-scaled))

(defn ^BufferedImage crop-image
  "Crop a rectangular area from an image. You need to sp"
  ([^BufferedImage src width height]
   (crop-image src width height {}))
  ([^BufferedImage src width height {:keys [x y] :or {x 0 y 0}}]
   (.getSubimage src x y width height)))


(defn write-image
  "Write a buffer image to file"
  [^BufferedImage src ^String format filename]
  {:pre [(or (= format "png")
             (= format "jpg"))]}
  (ImageIO/write src format (io/file (str filename "." format))))

(defn ^BufferedImage read-image
  "Read image from filesystem. Return BufferedImage"
  [filename]
  (ImageIO/read (io/file filename)))


(defn- crop-for-concat
  "Crop an image to the correct size for a concat for a thumbnail"
  [^BufferedImage src]
  (let [width (.getWidth src)
        height (.getHeight src)]
    (crop-image src (/ width 2) height {:x (/ width 7)})))


(defn ^BufferedImage concat-images
  "Concat a new image that contains two images"
  [^String left-path ^String right-path]
  (let [left (read-image left-path)
        right (read-image right-path)
        type (.getType left)
        width (.getWidth left)
        height (.getHeight left)
        concat-image (BufferedImage. width height type)
        ^Graphics2D g2d (.createGraphics concat-image)]
    (.drawImage g2d ^BufferedImage (crop-for-concat left) 0 0 nil)
    ; Draw second image from the second half
    (.drawImage g2d ^BufferedImage (crop-for-concat right) (int (/ width 2)) 0 nil)
    (.dispose g2d)

    concat-image))


(comment

  (merge-images "resources/image.png" "resources/image2.png")

  (resize-image (ImageIO/read (io/file "resources/image.png")) {:width 500 :height 500})


  (.exists (io/file "resources/image.png"))
  (ImageIO/read (File. "resources/image.png")))
