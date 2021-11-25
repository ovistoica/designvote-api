(ns designvote.http
  (:require
    [muuntaja.core :as m]))


(defn handle-response [{:keys [status] :as response} handle-error]
  (let [body (m/decode-response-body response)]
    (if (< status 300)
      (assoc body :status status)
      (handle-error (assoc body :status status)))))
