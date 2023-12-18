/* 
 * Copyright 2020-2022 JackTrip Labs, Inc.
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
 * OutputBusMixer: creates a "JackTripDownMixOut" synth, which downmixes the
 * audio from all input busses and sends it to all clients.
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \preChain: signal processing chain applied to each client's audio before it is sent to the bus
 * \postChain: signal processing chain applied to flattened audio before sending to each personal mix
 * \broadcast: if true, create a 2-channel mix for recording and broadcast
 */

OutputBusMixer : InputBusMixer {
    var <>broadcast = false;

    // create a new instance
    *new { | maxClients = 16 |
        ^super.new(maxClients);
    }

    // starts up all the audio on the server
    start {
        var b, g, node;
        var synthName = "JackTripDownMixOut";
        var postChainName, postChainSynthName;
        var args = [\speakerDelay, jamulusDelay];

        // use alternate synth if broadcast is true
        if (broadcast, {
            synthName = "BroadcastMixOut";
        });

        // start input bus mixer first
        super.start();

        // execute postChain before actions
        if(bypassFx==1, {
            postChainName = "";
            postChainSynthName = "";
        }, {
            postChainName = postChain.getName();
            postChainSynthName = postChain.getSynthName();
            postChain.before(server);
        });

        // use group 200 for client output synths
        g = ParGroup.basicNew(server, 200);

        // create a bundle of commands to execute
        b = server.makeBundle(nil, {
            this.sendSynthDef(synthName, synthName ++ postChainSynthName);

            // use group 100 for client input synths and use group 200 for client output synths
            // p_new is a server command (see Server Command Reference on SC documentation)
            // that creates a parallel group, which represents a set of Synths that execute
            // simultaneously
            server.sendMsg("/p_new", 200, 1, 0);
        });

        // wait for server to receive bundle
        server.sync(nil, b);

        // create a single mix and outputs to all clients including jamulus
        if(bypassFx==1, {
            node = Synth(synthName, args, g, \addToTail);
        }, {
            args = args ++ postChain.getArgs();
            node = Synth(synthName ++ postChainSynthName, args, g, \addToTail);
            // execute postChain after actions
            postChain.after(server, node);
        });
        ("Created synth" + (synthName ++ postChainSynthName) + postChainName + node.nodeID).postln;

        // signal that the mix has started
        // signal is defined in the BaseMix class and represents a Condition object
        // after these two lines are executed, the BaseMix knows that the
        // proper Synths have been set up, and can execute other routines
        this.mixStarted.test = true;
        this.mixStarted.signal;
    }
}
