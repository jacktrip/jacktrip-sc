# JackTrip Mixers for SuperCollider

_Copyright (c) 2020-2021 JackTrip Labs, Inc._

_Licensed under the Apache License 2.0. Please see [LICENSE](LICENSE) for more information._

This repository contains source code used to run SuperCollider on
JackTrip Virtual Studios.

For local development and testing, clone this repository into your
SuperCollider Extensions directory, or any other location that is
loaded by your sclang interpreter.


## Mixers

Mixers are used to process audio for all JackTrip and Jamulus clients.
The `mixers` directory contains one subdirectory for each type of mixer.
Mixers always take the maximum number of clients as an argument to `new`.
For example, to create a simple mixer that supports up to 10 clients,
use `SimpleMix(10)`.

An assumption is currently made that every client will have two audio input
channels and two audio output channels. You must run SuperCollider with
enough channels (and for some mixers, busses) to accomodate the maximum
number of clients. For example, 10 clients requires 20 input channels and
20 output channels. You can configure your server to support this by
modifying your `startup.scd` file:

```
Server.default.options.numInputBusChannels_(20).numOutputBusChannels_(20);
```

Be very careful not to create mixers with `maxClients` greater than your
number of input and output channels. This will cause it to wrap-around,
leading to unnatural results.


## BaseMix

All mixers extend the `BaseMix` class, which contains several common methods:

* `connect`: establishes a connection to your audio server. By default, this
will be your local server, but you can also connect to remote servers by
setting `serverIp`.

* `wait`: this will block until the mixer has started processing audio.

* `runAfter`: this can be used to provide a function that is executed after
the mixer has started processing audio.

By convention, mixers should also implement a `start` method which starts
processing audio.

For example, to start a simple mixer on your local SuperCollider server for
two clients, run:

```
SimpleMix(2).connect.start;
```

To start a simple mixer on a remote audio server with IP address 10.20.1.10:

```
SimpleMix(2).serverIp_("10.20.1.10").connect.start;
```

Note that you can use `serverIp` to connect to and run mixers on remote
virtual studios.


## Using SynthDefs

It is often useful to share the code for synthdefs. The `links` and
`functions` directories contain some common code that may be useful.

Additionally, we have established a convention that makes it easier
for mixers to load and send synthdefs. The `synthdefs` directory has
one file per definition. Each file includes a function named `~synthDef`.

The `BaseMix` class implements a `useSynthDef` (and `useSynthDefs` plural)
method. It takes a string that is the name of a synthdef; it should match
the name of a file in the `synthdefs` directory. This implements a simple
cache that avoids having to resend the definitions repeatedly. To disable
it, set `useSynthCache_(false)`.
