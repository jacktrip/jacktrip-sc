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
 * BaseMixer: base class for other JackTrip Virtual Studio mixers
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 */

BaseMixer : Object {
    var <>maxClients;
    var <>withJamulus = true;		// create mixes adapted Jamulus being connected on channels 1 & 2
    var <>useSynthCache = true;		// send each synth definition to the server at most one time
    var <>masterVolume = 1.0;		// master volume level multiplier
    var <>selfVolume = 1.0;         // sets the default volume level that each client will hear themselves at (requires personal mixes)
    var <>serverIp = "127.0.0.1";	// IP address or hostname of remote audio server
    var <>serverPort = 57110;		// port number of remote audio server (default SC port)
    var <>serverReady, <>server;    // state of the server and the server object
    var <>mixStarted;				// Condition object used to pause execution until the server is ready
    var <>defaultMix;				// default master mix is just an array of ones (do nothing)
    var <>bypassFx;                 // bypass FX processing (skip preChain and postChain)
    var <>preChain, <>postChain;	// signal chains for processing audio before (pre) and after (post) mixing down to stereo
    classvar inputChannelsPerClient = 2;	// for stereo audio inputs
    classvar outputChannelsPerClient = 2;	// for stereo audio outputs

    // create a new instance
    *new { | maxClients = 16 |
        ^super.newCopyArgs(maxClients).serverReady_(Condition.new).mixStarted_(Condition.new).defaultMix_(1 ! maxClients)
            .bypassFx_(false).preChain_(SignalChain.new).postChain_(SignalChain.new);
    }

    // connect to a remote server
    connect {
        var waitForServer = Routine {
            var retries = 10;
            server.notify;
            server.initTree;

            // Make 10 attempts to connect to the server
            {retries > 0}.while({
                //server.serverRunning.postln;
                //Server.allRunningServers.postln;
                if (Server.allRunningServers != [], {
                    "Server started".postln;
                    retries = 0;
                }, {
                    "Waiting for server".postln;
                    retries = retries - 1;
                    1.sleep;
                });
            });

            serverReady.test = true;
            serverReady.signal;
        };

        // Create a new server object using the IP address and Port
        // server can be used as one might use the 's' global variable locally
        ("Connecting to server"+serverIp++":"++serverPort).postln;
        // explicitly include empty ServerOptions to work-around 3.12 bug #5568
        Server.default = server = Server.remote(\remote, NetAddr(serverIp, serverPort), ServerOptions.new);
        server.doWhenBooted({waitForServer.value});
    }

    // sendSynthDef method sends a synth definition from a file to the server for use in audio mixing
    sendSynthDef { | srcName, name = "" |
        var defPath;

        if (name == "", { name = srcName; });

        defPath = SynthDef.synthDefDir;
        defPath = defPath ++ name ++ ".scsyndef";

        // if the synthdef file exists already, no need to do anything,
        // since Synthdefs in the default directory are automatically loaded
        // by the server on boot. Otherwise, write it to disk at the default
        // location and send it to the server.
        if (useSynthCache && File.exists(defPath), {
            ("Reusing cached SynthDef:" + name).postln;
        
            // load synthdef into global lib (needed by VSTPlugin and other things)
            SynthDescLib.global.read(defPath);
        }, {
            var sdef;
            var myPreChain = preChain;
            var myPostChain = postChain;

            if(bypassFx==1, {
                myPreChain = SignalChain.new;
                myPostChain = myPreChain;
            });

            (this.class.filenameSymbol.asString.dirname +/+ "../../synthdefs/" ++ srcName ++ ".scd").load;
            sdef = SynthDef(name, {
                SynthDef.wrap(~synthDef, prependArgs: [maxClients, myPreChain, myPostChain, inputChannelsPerClient, outputChannelsPerClient, withJamulus]);
            });

            if (server.isLocal, {
                ("Loading SynthDef:" + name).postln;
                sdef.load(server);
            }, {
                ("Sending SynthDef:" + name).postln;
                sdef.writeDefFile;
                sdef.send(server);
            });
        });
    }

    // wait for mix to start
    // mixStarted is the Condition object and pauses execution until
    // the value is set to true and mixStarted is signalled
    wait {
        mixStarted.wait;
    }

    // run a routine after mix has started
    // mixStarted must have value of true and must be signalled
    // in order for r to run
    after { | r |
        Routine {
            this.wait;
            r.value;
        }.run;
    }
}
