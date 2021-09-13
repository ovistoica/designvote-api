(ns designvote.media.aws
  "Management of aws resources. We use DigitalOcean Spaces
  to store design images but we manage that with the sdk for
  aws S3"
  (:require [designvote.config :refer [config]]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import (com.amazonaws.services.s3 AmazonS3ClientBuilder AmazonS3Client)
           (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
           (com.amazonaws.auth AWSStaticCredentialsProvider BasicAWSCredentials)))

(def ^:private ^String access-key (:aws-access-key config))
(def ^:private ^String secret-key (:aws-secret-key config))
(def ^:private ^String bucket (:aws-s3-bucket-name config))
(def ^:private ^String endpoint (:aws-s3-endpoint config))


(def service-endpoint (str "https://" endpoint))
(def sign-region (first (string/split endpoint #"\.")))

(defonce ^AmazonS3Client s3-client (-> (AmazonS3ClientBuilder/standard)

                                       (.withEndpointConfiguration
                                         (AwsClientBuilder$EndpointConfiguration. service-endpoint sign-region))

                                       (.withCredentials
                                         (AWSStaticCredentialsProvider. (BasicAWSCredentials. access-key secret-key)))

                                       (.build)))


(defn put-file [^String file-name ^String file-path]
  "Upload a file to the DO Spaces storage"
  (when-let [f (io/file file-path)]
    (when (.exists f))
    (.putObject s3-client bucket file-name f)))

(comment


  (put-file "test-de-smecheri" "resources/image.png"))
