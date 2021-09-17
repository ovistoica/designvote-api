(ns designvote.media.util
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [image-resizer.crop :as c]
            [image-resizer.resize :as r])
  (:import (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (java.io File ByteArrayOutputStream ByteArrayInputStream InputStream)
           (javax.imageio.stream ImageInputStream)
           (java.net URL)
           (org.imgscalr Scalr)))

(def thumb-width "Final width of a design thumbnail" 300)
(def thumb-height "Final height of a design thumbnail" 225)


(defn write-image
  "Write a buffer image to file."
  [^BufferedImage src filename & {:keys [format] :or {format :jpg}}]
  (ImageIO/write src (name format) (io/file filename)))

(defmulti read-image (fn [input & _] input))

(defmethod read-image :file
  [_ ^File file]
  (ImageIO/read file))

(defmethod read-image :path
  [_ path]
  (ImageIO/read (io/file path)))

(defn dimensions [^BufferedImage image]
  [(.getWidth image) (.getHeight image)])

(defn get-format [content-type]
  (-> content-type
      (string/split #"/")
      (second)
      (keyword)))

(defn get-img-ratio
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
    String (read-image :path image)
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

(defn thumbnail [img]
  (let [half-thumbnail (/ thumb-width 2)
        x-offset (/ (- thumb-height half-thumbnail) 2)]
    (-> img
        (square)
        ((r/resize-width-fn thumb-height))
        (crop-image half-thumbnail thumb-height {:x x-offset}))))


(comment
  (write-image
    (thumbnail
      (buffered-image "resources/lion_king3.jpeg"))
    "resources/lion_thumb3.jpeg"))

(comment
  (def img1 (buffered-image "resources/skull_thumb1.jpeg"))
  (def img2 (buffered-image "resources/skull_thumb2.jpeg"))
  (time (square img))
  (dimensions img)
  (dimensions ((r/resize-width-fn 180) (square img))))

