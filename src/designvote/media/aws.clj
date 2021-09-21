(ns designvote.media.aws
  "Management of aws resources. We use DigitalOcean Spaces
  to store design images but we manage that with the sdk for
  aws S3"
  (:require [designvote.config :refer [config]]
            [clojure.string :as string]
            [designvote.media.core :as img]
            [clojure.java.io :as io])
  (:import (com.amazonaws.services.s3 AmazonS3ClientBuilder AmazonS3Client)
           (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
           (com.amazonaws.auth AWSStaticCredentialsProvider BasicAWSCredentials)
           (java.io File ByteArrayOutputStream ByteArrayInputStream)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)
           (com.amazonaws.services.s3.model ObjectMetadata PutObjectRequest CannedAccessControlList)))

(def ^:private ^String access-key (:aws-access-key config))
(def ^:private ^String secret-key (:aws-secret-key config))
(def ^:private ^String bucket (:aws-s3-bucket-name config))
(def ^:private ^String endpoint (:aws-s3-endpoint config))
(def ^:private ^String cdn-endpoint (:do-cdn-endpoint config))


(def service-endpoint (str "https://" endpoint))
(def sign-region (first (string/split endpoint #"\.")))

(defonce ^AmazonS3Client s3-client
         (-> (AmazonS3ClientBuilder/standard)

             (.withEndpointConfiguration
               (AwsClientBuilder$EndpointConfiguration. service-endpoint sign-region))

             (.withCredentials
               (AWSStaticCredentialsProvider. (BasicAWSCredentials. access-key secret-key)))

             (.build)))

(defn- ^File file [input]
  "Return a File instance based on input"
  (condp instance? input
    String (io/file input)
    File input))

(defn- put-request [^BufferedImage input ^String name]
  "Creates a PutObjectRequest instance for an image and the new object name"
  (let [os (doto (ByteArrayOutputStream.))]
    (ImageIO/write (img/check-img input :jpeg) "jpeg" os)
    (let [buff (.toByteArray os)
          is (ByteArrayInputStream. buff)
          meta (doto (ObjectMetadata.)
                 (.setContentType "image/jpeg")
                 (.setContentLength (count buff)))]
      (doto (PutObjectRequest. bucket name is meta)
        (.withCannedAcl CannedAccessControlList/PublicRead)))))


(defn image-url [^String name]
  "Build the access url of a file name from cdn"
  (str "https://" bucket "." cdn-endpoint "/" name))

(defn upload-image! [^BufferedImage input ^String name]
  "Upload an image to the S3 managed Digital Ocean Spaces"
  (let [req (put-request input name)]
    (.putObject s3-client req)
    (image-url name)))


(defn put-file [^String file-name input]
  "Upload a file to the DO Spaces storage"
  (let [f (file input)]
    (when (.exists f)
      (.putObject s3-client bucket file-name f))))

(comment
  (upload-image!
    (img/buffered-image "resources/image.png")
    "jiu-jitsu-test4.jpg"))
