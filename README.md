# quux

An example @bobbyno and I cooked up while comparing
Clojure+Langohr to Java+RabbitMQ. It does some things
differently than the official Langohr examples.

Note: this example temporarily uses a patched example of Langohr. It
has:

* print statements for consumer events
* A fix to the handling of `subscribe` keys like
     :handle-consume-ok. The bug has been fixed but isn't yet
     in Clojars.


The project uses [Midje](https://github.com/marick/Midje/).

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.
