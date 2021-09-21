(ns designvote.media.core
  "Helpful functions to deal with images and other media"
  (:require [image-resizer.resize :as r]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (javax.imageio ImageIO ImageWriter ImageWriteParam IIOImage)
           (java.awt.image BufferedImage)
           (java.io ByteArrayOutputStream OutputStream ByteArrayInputStream File InputStream)
           (javax.imageio.stream ImageInputStream)
           (java.net URL)))

(def thumb-width "Final width of a design thumbnail" 300)
(def thumb-height "Final height of a design thumbnail" 225)


(defn write-image
  "Write a buffer image to file."
  [^BufferedImage src filename & {:keys [format] :or {format :jpg}}]
  (ImageIO/write src (name format) (io/file filename)))


(defn dimensions [^BufferedImage image]
  [(.getWidth image) (.getHeight image)])

(defn get-format [content-type]
  "Ex: image/png -> :png"
  (-> content-type
      (string/split #"/")
      (second)
      (keyword)))

(defn ensure-opaque
  "When writing to jpeg format, images must not have transparency."
  [^BufferedImage img]
  (if (= (.getTransparency img) BufferedImage/OPAQUE)
    img
    (let [[w h] (dimensions img)
          pixels (.getRGB img 0 0 w h nil 0 w)]
      (doto (BufferedImage. w h BufferedImage/TYPE_3BYTE_BGR)
        (.setRGB 0 0 w h pixels 0 w)))))

(defn ^BufferedImage check-img [^BufferedImage img format]
  (if (contains? #{:jpg :jpeg} format)
    (ensure-opaque img)
    img))

(defn- stream->buff-img [^ByteArrayOutputStream os]
  "Convert a OutputStream to a BufferedImage"
  (let [bytes (.toByteArray os)
        bais (ByteArrayInputStream. bytes)]
    (ImageIO/read bais)))


(defn ^BufferedImage buffered-image [image]
  "Return a BufferedImage instance based on image input"
  (condp instance? image
    BufferedImage image
    String (ImageIO/read (io/file image))
    ByteArrayOutputStream (stream->buff-img image)
    File (ImageIO/read ^File image)
    InputStream (ImageIO/read ^InputStream image)
    ImageInputStream (ImageIO/read ^ImageInputStream image)
    URL (ImageIO/read ^URL image)))


(defn crop-image
  "Crop a rectangular area from an image. Returns a new BufferedImage"
  ([^BufferedImage src width height]
   (crop-image src width height {}))
  ([^BufferedImage src width height {:keys [x y] :or {x 0 y 0}}]
   (.getSubimage src x y width height)))


(defn square [^BufferedImage img]
  (let [[w h] (dimensions img)]
    (when (= w h) img)
    (if (> w h)
      (let [x-offset (/ (- w h) 2)]
        (crop-image img h h {:x x-offset}))
      (let [y-offset (/ (- h w) 2)]
        (crop-image img w w {:y y-offset})))))

(defn thumbnail-chunk [img]
  "Resize an image to be half of thumbnail."
  (let [half-thumbnail (/ thumb-width 2)
        x-offset (/ (- thumb-height half-thumbnail) 2)]
    (-> img
        (square)
        ((r/resize-width-fn thumb-height))
        (crop-image half-thumbnail thumb-height {:x x-offset}))))

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


(defn concat-images
  "Create a new image from two images of the same size"
  [image1 image2 & {:keys [width height] :or {width  thumb-width
                                              height thumb-height}}]
  (let [hw (/ width 2)
        concat (BufferedImage. width height BufferedImage/TYPE_3BYTE_BGR)]
    (doto (.createGraphics concat)
      (.drawImage ^BufferedImage image1 0 0 nil)
      (.drawImage ^BufferedImage image2 hw 0 nil)
      (.dispose))
    concat))

(comment

  (concat-images (buffered-image "resources/lion_king1.jpeg")
                 (buffered-image "resources/lion_king2.jpeg")))
