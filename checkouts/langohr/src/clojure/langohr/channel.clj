;; Copyright (c) 2011-2013 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns langohr.channel
  (:import [com.rabbitmq.client ConnectionFactory Connection Channel]
           com.novemberain.langohr.channel.FlowOk))


;;
;; API
;;

(defn ^Channel open
  "Opens a new channel on given connection using channel.open AMQP method"
  ([^Connection connection]
     (.createChannel connection))
  ([^Connection connection id]
     (.createChannel connection id)))


(defn close
  "Closes given channel using channel.close AMQP method"
  ([^Channel channel]
     (.close channel))
  ([^Channel channel ^long code ^String message]
     (.close channel code message)))


(defn open?
  "Checks if channel is open. Consider using langohr.core/open? instead."
  [^Channel channel]
  (.isOpen channel))
(def closed? (complement open?))


(defn ^Boolean flow?
  "Returns true if flow is active on given channel. Uses channel.flow AMQP method."
  ([^Channel channel]
     (.getActive (.getFlow channel))))


(defn ^com.novemberain.langohr.channel.FlowOk flow
  "Enables or disables channel flow using channel.flow AMQP method"
  ([^Channel channel ^Boolean on]
     (FlowOk. (.flow channel on))))
