# cljprj

A Clojure Project

## Usage

    lein deps # if it's the first time
    lein run -m cljprj.web

    lein ring server-headless 8080

This will run on port 8080.  You can override that with an environment var called `PORT`

You will need mongodb running, the default connection string is:

    mongodb://cljprj:cljprj@localhost:27017/cljprj

So your mongo will need to be running on that port with that user available.  You can override the default connection
string by setting an environment variable `MONGOHQ_URL` to whatever you want (user/pass is mandatory though).

## Tests

to run tests use:

    lein midje

Some of the tests are acceptance tests so you will need to be running the app somewhere.  Set the base url to run tests
against with an environment var called `BASE_URL`.

It is possible to specify which tests to run by specifying the whole namespace, ie:

    lein midje cljprj.test.unit

or

    lein midje cljprj.test.acceptance

## License

Copyright © 2011 Matthew Gilliard (mjg123) & Andrew Jones (ahjones)

Distributed under the Eclipse Public License, the same as Clojure.
