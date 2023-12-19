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
 * InputBusMixer: creates a synth that feeds audio from each client into
 *                private input buses
 *
 * Channels Layout:
 *		Hardware output buses:
 * 			0 through 
 *				outputChannelsPerClient x maxClients - 1
 *
 *		Hardware input buses:
 *			outputChannelsPerClient * maxClients through 
 *				(outputChannelsPerClient + inputChannelsPerClient) x maxClients - 1
 *
 *		Private channels:
 *			(outputChannelsPerClient + inputChannelsPerClient) x maxClients and above
 *
 *
 * All buses in SuperCollider are single-channel buses. The defaults for 
 * outputChannelsPerClient and inputChannelsPerClient are 2 (for stereo audio). 
 * Each client thus requires 4 non-private channels: two for output and two 
 * for input. When reading inputs and outputs in SynthDefs, the SoundIn UGen's 
 * 0-index starts at the first hardware input bus. On the other hand, the In 
 * UGen's 0-index starts at the first hardware output bus. The Out UGen's 
 * 0-index starts at the first hardware output bus as well.
 *
 * Internally, a "JackTripToInputBus" Synth is created, which reads stereo audio from
 * the hardware input buses to the corresponding private buses, which are also stereo
 * (see the global variable ~inputBuses created by ~makeInputBuses, which is an array
 * of Bus objects that automatically allocates the correct number of private channels).
 * The variable ~inputBuses is used to route audio signals from the hardware input channels
 * to the private channels for each client.
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
 * \jamulusDelay : delay added to dry jamulus signal, in seconds
 */

InputBusMixer : BaseMixer {
    var <>jamulusDelay = 0;

    // create a new instance
    *new { | maxClients = 16 |
        ^super.new(maxClients);
    }

    // starts up JackTripToInputBus synth (must be run from a Routine)
    start {
        var node, g, b, args;
        var jacktripSynthName = "JackTripToInputBus";
        var jamulusSynthName = "JamulusToInputBus";
        var speakerSynthName = "SpeakerToInputBus";
        var preChainName, preChainSynthName;

        // wait for server to be ready
        serverReady.wait;

        // execute preChain before actions
        if(bypassFx==1, {
            preChainName = "";
            preChainSynthName = "";
        }, {
            preChainName = preChain.getName();
            preChainSynthName = preChain.getSynthName();
            preChain.before(server);
        });

        // g represents a node group
        // a group represents a set of Synths running on the server
        // two groups are used, one for input Synths (100) and one for output Synths (200)
        g = ParGroup.basicNew(server, 100);

        // create a bundle of commands to execute
        b = server.makeBundle(nil, {
            // make input busses
            (this.class.filenameSymbol.asString.dirname +/+ "../../functions/makeInputBuses.scd").load;
            ~makeInputBuses.value(server, maxClients, inputChannelsPerClient, outputChannelsPerClient);

            // initialize ATK decoder
            (this.class.filenameSymbol.asString.dirname +/+ "../../functions/initATK.scd").load;
            ~initATKDecoderLargePinnaeDummy.value(server);
        });

        // wait for server to receive bundle
        server.sync(nil, b);

        // create a bundle of commands to execute
        b = server.makeBundle(nil, {
            // create synthdef to send audio to the input busses
            this.sendSynthDef(jacktripSynthName, jacktripSynthName ++ preChainSynthName);
            this.sendSynthDef(speakerSynthName);
            if (withJamulus, {
                this.sendSynthDef(jamulusSynthName, jamulusSynthName ++ preChainSynthName);
            });
            
            // free any existing nodes
            "Freeing all nodes...".postln;
            server.freeAll;

            // use group 100 for client input synths and use group 200 for client output synths
            // p_new is a server command (see Server Command Reference on SC documentation)
            // that creates a parallel group, which represents a set of Synths that execute
            // simultaneously
            server.sendMsg("/p_new", 100, 1, 0);
        });

        // wait for server to receive bundle
        server.sync(nil, b);

        // create synths to send audio to the input busses
        args = [\mix, defaultMix, \mul, masterVolume];

        // create dry speaker synth
        node = Synth(speakerSynthName, args, g, \addToTail);
        ("Created synth" + speakerSynthName + node.nodeID).postln;

        if(bypassFx==1, {
            // create dry jacktrip synth
            node = Synth(jacktripSynthName, args, g, \addToTail);
            ("Created synth" + (jacktripSynthName ++ preChainSynthName) + preChainName + node.nodeID).postln;

            if (withJamulus, {
                // create dry jamulus synth
                args = args ++ [\delay, jamulusDelay];
                node = Synth(jamulusSynthName, args, g, \addToTail);
                ("Created synth" + (jamulusSynthName ++ preChainSynthName) + preChainName + node.nodeID).postln;
            });
        }, {
            // create jacktrip synth with effects
            args = args ++ preChain.getArgs();
            node = Synth(jacktripSynthName ++ preChainSynthName, args, g, \addToTail);
            ("Created synth" + (jacktripSynthName ++ preChainSynthName) + preChainName + node.nodeID).postln;
    
            if (withJamulus, {
                // always default pan Jamulus to center if using PanningLink
                args = args ++ [\delay, jamulusDelay];
                // create jamulus synth with effects
                node = Synth(jamulusSynthName ++ preChainSynthName, args, g, \addToTail);
                ("Created synth" + (jamulusSynthName ++ preChainSynthName) + preChainName + node.nodeID).postln;
            });

            // execute preChain after actions
            preChain.after(server, node);
        });
    }

    // stop all audio on the server
    // frees all Synth nodes
    stop {
        server.freeAll;
    }
}
