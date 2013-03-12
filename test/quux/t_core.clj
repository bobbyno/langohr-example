(ns quux.t-core
  (:use midje.sweet)
  (:use [quux.core])
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [langohr.core :as rmq]
            [langohr.basic :as lb]))

(defn to-rfc822-date-string [datetime]
  (format/unparse (format/formatters :rfc822) datetime))

(fact "can convert a map to a map with a Jodatime timestamp"
  (let [input-date (time/date-time 2002 3 4)
        input-string (to-rfc822-date-string input-date)
        input-map {:timestamp input-string :other-key "other value"}]
    (convert-embedded-timestamp input-map) => {:timestamp input-date :other-key "other value"}))

(fact-group :integration

  (fact "`make-endpoints` provides a close function"
    (let [endpoints (make-endpoints configuration)]
      ((:close endpoints))
      (:channel endpoints) => rmq/closed?
      (:connection endpoints) => rmq/closed?))

  (fact "`launch-asynchronous-handler` also allows closing"
    (let [endpoints (launch-asynchronous-handler configuration (fn [& args]))]
      ((:close endpoints))
      (:channel endpoints) => rmq/closed?
      (:connection endpoints) => rmq/closed?
      (.isAlive (:thread endpoints)) => false))

  (fact "sending a message through AMQP."
    (let [promise (promise)]
      (launch-asynchronous-string-handler configuration (partial deliver promise))
      ( (make-string-sender configuration) "string")
      (deref promise 2000 :timeout) => "string"))

)
