(defproject quux "0.0.1-SNAPSHOT"
  :description "Try out AMQP with Langohr"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-time "0.4.4"]
                 [com.novemberain/langohr "1.0.0-beta13"]]
  :main quux.core
  :profiles {:dev {:dependencies [[midje "1.5-beta3"]]}})
