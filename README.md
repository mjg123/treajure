# cljprj

A Clojure Project

## Usage

    lein deps # if it's the first time
    lein run -m cljprj.web

to run tests use:

    lein midje

But some of the tests are acceptance tests so you will need to be running `lein ring` somewhere else.  It is possible to specify which tests
to run by specifying the whole namespace, ie:

    lein midge cljprj.test.unit

or

    lein midge cljprj.test.acceptance

## License

Copyright (C) 2011 Matthew Gilliard (mjg123) & Andrew Jones

Distributed under the Eclipse Public License, the same as Clojure.
