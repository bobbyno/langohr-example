(ns quux.core
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange  :as le]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))

(defn convert-embedded-timestamp [data]
  (update-in data [:timestamp] #(format/parse (format/formatters :rfc822) %) ))

(def configuration {:exchange-name "events"
                    :exchange-type "direct"
                    :exchange-connection-uri (or (System/getenv "CLOUDAMQP_URL") "amqp://guest:guest@localhost")
                    :queue-name "user.profile"
                    :routing-key "user.profile"
                    :durable? true
                    :exclusive? false
                    :auto-delete? false
                    :auto-ack? false
                    :x-ha-policy "all"})

;;; Changes here:
;;; 1. Using a macro was actually wrong: we didn't need to wrap a body
;;;    of code, we just needed to construct and return a channel.
;;; 2. It's better to pass the configuration in than to rely on global state.
;;; 3. It returns a map with all the endpoints and a closer function.
;;;    Some of the endpoints are only used for testing (to check if they've all
;;;    been closed.

(defn make-endpoints [{:keys [exchange-name exchange-type routing-key queue-name durable? exclusive? auto-delete? x-ha-policy]}]
  (let [connection (rmq/connect)
        channel (lch/open connection)
        queue (.getQueue (lq/declare channel queue-name
                                     :durable durable? :exclusive exclusive? :auto-delete auto-delete?
                                     :arguments {"x-ha-policy" x-ha-policy}))]
     (le/declare channel exchange-name exchange-type :durable durable?)
     (lq/bind channel queue exchange-name :routing-key routing-key)
     {:connection connection
      :channel channel
      :queue queue
      :close (fn []
               (rmq/close channel)
               (rmq/close connection))}))

(defn launch-asynchronous-handler [{:keys [queue-name auto-ack?] :as configuration} handler]
  (let [consumer-tag (str (gensym "consumer-"))
        consumer-approved? (promise)
        endpoints (make-endpoints configuration)
        thread (Thread. #(lc/subscribe (:channel endpoints) queue-name handler :auto-ack auto-ack?
                                       :consumer-tag consumer-tag
                                       :handle-consume-ok (fn [& args] (deliver consumer-approved? true))
                                       :handle-shutdown-signal (fn [& args] (prn "shutdown caught" args))
                                       :handle-recover-ok (fn [& args] (prn "recover caught" args))))]
    (.start thread)
    (if (deref consumer-approved? 5000 false)
      (assoc endpoints
             :thread thread
             :close (fn []
                      (lb/cancel (:channel endpoints) consumer-tag)
                      ((:close endpoints))
                      (.interrupt thread)))
      (println "Something is wedged. The consumer was never approved:" consumer-tag))))

(defn launch-asynchronous-string-handler [configuration handler]
  (launch-asynchronous-handler configuration
                               (fn [channel metadata payload] (handler (String. payload "UTF-8")))))


;;; Even though we only send one string in the first test, I want to
;;; separate making a connection from sending a message.
;;;
;;; Also, even though the only use of sending messages is in tests, I
;;; think it makes sense to put this code in src. It is working code we
;;; could potentially use.
;;;
;;; This makes a function that encapsulates the channel to send strings to. 

(defn make-string-sender [{:keys [exchange-name routing-key] :as configuration}]
  (let [endpoints (make-endpoints configuration)]
    (fn [payload]
      (lb/publish (:channel endpoints) exchange-name routing-key payload :content-type "text/plain"))))
