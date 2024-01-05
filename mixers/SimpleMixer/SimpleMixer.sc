/* 
 * Copyright 2020-2021 JackTrip Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * SimpleMixer: a minimal mixer that scales well
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \serverIp: IP address or hostname of remote audio server
 * \serverPort: port number of remote audio server
 */
 
SimpleMixer : BaseMixer {

    // create a new instance
    *new { | maxClients = 16 |
        ^super.new(maxClients);
    }

    // starts up all the audio on the server
    start {		
        var b, g, node;

        // wait for server to be ready
        serverReady.wait;

        // use group 100 for client output synths
        g = ParGroup.basicNew(server, 100);

        // create a bundle of commands to execute
        b = server.makeBundle(nil, {
            // send synthDefs
            this.sendSynthDef("JackTripSimpleMix");

            // free any existing nodes
            "Freeing all nodes...".postln;
            server.freeAll;

            // create group 100 for client output synths
            server.sendMsg("/p_new", 100, 1, 0);
        });

        // wait for server to receive bundle
        server.sync(nil, b);

        // create output for all jacktrip clients that includes jamulus
        node = Synth("JackTripSimpleMix", [\mix, defaultMix, \mul, masterVolume], g, \addToTail);
        ("Created synth JackTripSimpleMix" + node.nodeID).postln;

        // add osc paths for the mixer
        OSCFunc({ |args|
            if (args.size == 3, {
                ("input: setting" + args[1] + "to" + args[2]).postln;
                node.set(args[1], args[2]);
            });
        }, "/input");

        // signal that the mix has started
        this.mixStarted.test = true;
        this.mixStarted.signal;
    }

    // stop all audio on the server
    stop {
        server.freeAll;
    }
}
