(ns designvote.media.core
  "Helpful functions to deal with images and other media"
  (:require [designvote.media.util :refer :all]
            [image-resizer.resize :as r]
            [image-resizer.crop :as c]
            [clojure.java.io :as io])
  (:import (javax.imageio ImageIO ImageWriter ImageWriteParam IIOImage)
           (java.awt.image BufferedImage AffineTransformOp)
           (java.awt.geom AffineTransform)
           (java.io ByteArrayOutputStream OutputStream)))


(defn ^BufferedImage resize-image
  "Resize image based on fixed or maximum achievable width/height"
  [^BufferedImage img {:keys [width height
                              max-width max-height]}]
  (let [[img-w img-h] (dimensions img)
        ratio (get-img-ratio {:width     width :height height
                              :max-width max-width :max-height max-height
                              :img-width img-w :img-height img-h})
        ^BufferedImage img-scaled
        (if (= 1 ratio)
          img
          (let [scale (AffineTransform/getScaleInstance (double ratio) (double ratio))
                transform-op (AffineTransformOp. scale AffineTransformOp/TYPE_BILINEAR)]
            (.filter transform-op img
                     (BufferedImage. (* ratio img-w)
                                     (* ratio img-h)
                                     (.getType img)))))]
    img-scaled))



(defn convert [^BufferedImage src & {:keys [format] :or {format :jpg}}]
  (let [baos (ByteArrayOutputStream.)
        img (check-img src format)]
    (ImageIO/write img (name format) ^OutputStream baos)
    (buffered-image baos)))


(defn compress-image
  "Compress a BufferedImage to 0.6 original quality to reduce size.
   Returns a new BufferedImage. Original is untouched.

  Takes the following keys as options:
      :quality - for JPEG images, a number between 0 and 100"
  [^BufferedImage img]
  (let [baos (ByteArrayOutputStream.)
        ios (ImageIO/createImageOutputStream baos)
        iw (doto ^ImageWriter (first
                                (iterator-seq
                                  (ImageIO/getImageWritersByFormatName "jpeg")))

             (.setOutput ios))
        iw-param (doto ^ImageWriteParam (.getDefaultWriteParam iw)
                   (.setCompressionMode ImageWriteParam/MODE_EXPLICIT)
                   (.setCompressionQuality (float (/ 60 100))))
        iio-img (IIOImage. (check-img img :jpg) nil nil)]
    (.write iw nil iio-img iw-param)
    (buffered-image baos)))


(defn concat-images [image1 image2 & {:keys [width height] :or {width  thumb-width
                                                                height thumb-height}}]
  (let [hw (/ width 2)
        concat (BufferedImage. width height BufferedImage/TYPE_3BYTE_BGR)]
    (doto (.createGraphics concat)
      (.drawImage ^BufferedImage image1 0 0 nil)
      (.drawImage ^BufferedImage image2 hw 0 nil)
      (.dispose))
    concat))




(comment



  (eval (thumbnail (buffered-image "resources/skull_icecream1.jpeg")))
  (dimensions (thumbnail2 (buffered-image "resources/skull_icecream2.jpeg")))

  (write-image (concat-images
                 (thumbnail (buffered-image "resources/lion_king3.jpeg"))
                 (thumbnail (buffered-image "resources/lion_king4.jpeg")))
               "resources/final.jpeg")

  (create-design-thumbnail "resources/lion_king2.jpeg" "resources/lion_king3.jpeg"))

