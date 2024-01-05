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
 * PersonalMixer: creates personal mixes for up to 100 clients
 *
 * This creates a "JackTripPersonalMixOut" synth. Each client's mix (which
 * accounts for things like self-volume) is used to generate their unique
 * output audio. The mix for each client can also be thought of as an array
 * of weights for the channels each client should hear. The
 * "JackTripPersonalMixOut" Synth computes the weighted sum of all of the clients'
 * audio signals and the resulting 2-channel signal to that particular client's
 * hardware output channel.
 *
 * Note that SynthDefs are defined in the sendSynthDefs function, but are only
 * executed when the start function gets called. This may result in confusion, 
 * because the variables starting with a '\' within a SynthDef behave like function 
 * arguments, though they are not explicitly defined at the top of the SynthDef's function.
 * Values are passed as defined in the start function, as an array such as
 * [\var, var1_value, \var2, var2_value]
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \preChain: signal processing chain applied to each client's audio before it is sent to the bus
 * \postChain: signal processing chain applied to flattened audio before sending to each personal mix
 */

PersonalMixer : InputBusMixer {
    
    // create a new instance
    *new { | maxClients = 16 |
        ^super.new(maxClients);
    }

    // starts up all the audio on the server
    start {
        var b, g, p, node, personalMixes;
        var synthName = "JackTripPersonalMixOut";
        var postChainName, postChainSynthName;
        var args = [\speakerDelay, speakerDelay];

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

        // initialize personal mixes to use selfVolume
        // supercollider doesn't seem to support arrays of arrays as synthdef parameters
        // (likely a limitation of OSC) so instead we just have a single array with the
        // personal mixes for every client
        personalMixes = 1 ! (maxClients * maxClients);
        maxClients.do{ arg clientNum;
            personalMixes[(clientNum * maxClients) + clientNum] = selfVolume;
        };

        if (withJamulus, {
            // default selfVolume for Jamulus mix to zero
            personalMixes[0] = 0;
        });
        args = args ++ [\mix, personalMixes];

        // create personal mix for all jacktrip clients that includes jamulus
        // outputs to all clients including jamulus
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
