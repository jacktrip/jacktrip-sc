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
 * AutoPanMix: automatically pans clients across a stereo sound field,
 *             and supports personal mixes up to 100 clients
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
 * Internally, a "jacktrip_panned_in" Synth is created, which
 * reads stereo audio from the hardware input buses to the corresponding private
 * buses, which are also stereo (see the instance variable inputBuses, which is an array
 * of Bus objects that automatically allocates the correct number of private channels).
 * The variable inputBuses is used to route audio signals from the hardware input channels
 * to the private channels for each client. This is also where the panning process takes
 * place, using the LinLin UGen.
 *
 * If there are fewer than 100 clients, a "jacktrip_personalmix_out" Synth. Each client's
 * mix (which accounts for things like self-volume) is used to generate their unique
 * output audio. The mix for each client can also be thought of 
 * as an array of weights for the channels each client should hear. The
 * "jacktrip_personalmix_out" Synth computes the weighted sum of all of the clients'
 * audio signals and the resulting 2-channel signal to that particular client's
 * hardware output channel.
 *
 * If there are 100 clients or more, then no personal mixes are created. Rather, an
 * instance of "jamulus_simple_out" and only a single Synth instance of
 * "jacktrip_simple_out" is created for the server. These two synths essentially do the
 * same thing, but the "jamulus_simple_out" Synth excludes its own input (as an edge
 * case that is handled separately for channel 0). "jamulus_simple_out" creates one mix
 * that is sent to Jamulus on channel 0, and "jacktrip_simple_out" creates one mix that
 * is sent to all other clients. This only happens when there are greater than 100 clients
 * for performance reasons, though if there are fewer than 100 clients, personal mixes are
 * generated.
 *
 * Note that SynthDefs are defined in the sendSynthDefs function, but are only
 * executed when the start function gets called. This may result in confusion, 
 * because the variables starting with a '\' within a SynthDef behave like function 
 * arguments, though they are not explicitly defined at the top of the SynthDef's function.
 * Values are passed as defined in the start function, as an array such as
 * [\var, var1_value, \var2, var2_value]
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \panSlots: number of panning slots to use across a stereo sound field
 * \selfVolume: default volume level multiplier used for yourself in your personal mix, when enabled
 */

AutoPanMix : BaseMix {
	
	// the following parameters are instance variables
	// * autopan is a boolean that if true, will automatically pan clients across stereo field
	// * panSlots is the number of positions to use for panning, if autopan is true
	// * selfVolume sets the default volume level that each client will hear themselves at (requires personal mixes)
	// * hpf sets the default high-pass filter frequency used for all clients
	// * lpf sets the default low-pass filter frequency used for all clients
	// the '<' is shorthand for a getter method and '>' is shorthand for a setter method
	var <>autopan, <>panSlots, <>selfVolume, <>hpf, <>lpf;

	// inputBuses is an array (one per client) of stereo audio busses, after applying any processing
	var inputBuses;

	// create a new instance
	*new { | maxClients = 16, autopan = true, panSlots = 3, selfVolume = 1.0, hpf = 20, lpf = 20000 |
		^super.new(maxClients).autopan_(autopan).panSlots_(panSlots).selfVolume_(selfVolume).hpf_(hpf).lpf_(lpf);
	}

	// starts up all the audio on the server
	start {

		// g represents an identifier for a group number,
		// a group represents a set of Synths running on the server
		// two groups are used, one for input Synths (100) and one for output Synths (200)
		var g = 100;

		Routine {
			// wait for server to be ready
			serverReady.wait;

			// free any existing nodes
			server.freeAll;

			// send synthDefs
			(this.class.filenameSymbol.asString.dirname +/+ "makeInputBusses.scd").load;
			~makeInputBusses.value(server, maxClients, inputChannelsPerClient, outputChannelsPerClient);
			this.sendSynthDefs(this.class.filenameSymbol.asString.dirname +/+ "synthDefs.scd");
			
			//this.sendSynthDefs(this.class.filenameSymbol.asString.dirname +/+ "../SimpleMix/synthDefs.scd");
			
			// use group 100 for client input synths and use group 200 for client output synths
			// p_new is a server command (see Server Command Reference on SC documentation)
			// that creates a parallel group, which represents a set of Synths that execute
			// simultaneously
			server.sendMsg("/p_new", 100, 1, 0);
			server.sendMsg("/p_new", 200, 1, 0);

			// wait for server to receive synthdefs
			server.sync;

			if (autopan, {
				// Squash the clients tracks to mono, then pan them before they reach
				// the input buses.
				var p = PanningLink.autoPan(maxClients, panSlots);
				var node = Synth("JackTripPannedIn", [\pan, p, \low, hpf, \high, lpf], g, \addToTail);
				("Created synth" + "JackTripPannedIn" + node.nodeID).postln;
			}, {
				// do not pan clients and do not squash to mono.
				var node = Synth("JackTripSimpleIn", [\low, hpf, \high, lpf], g, \addToTail);
				("Created synth" + "JackTripSimpleIn" + node.nodeID).postln;
			});

			g = 200;

			// scsynth starts to max out a core after about 100 personal mixes,
			// and supernova throws mysterous bad_alloc errors
			if (maxClients > 100, {
				var node;
				node = Synth("JackTripSimpleMix", [\mix, defaultMix, \mul, masterVolume], g, \addToTail);
				("Created synth JackTripSimpleMix" + node.nodeID).postln;
			}, {

				var node;

				// initialize personal mixes to use selfVolume
				// supercollider doesn't seem to support arrays of arrays as synthdef parameters
				// (likely a limitation of OSC) so instead we just have a single array with the
				// personal mixes for every client
				var personalMixes = 1 ! (maxClients * maxClients);
				maxClients.do{ arg clientNum;
					personalMixes[(clientNum * maxClients) + clientNum] = selfVolume;
				};

				if (withJamulus, {
					// default selfVolume for Jamulus mix to zero
					personalMixes[0] = 0;
				});

				// create personal mix for all jacktrip clients that includes jamulus
				// outputs to all clients including jamulus
				node = Synth("JackTripPersonalMixOut", [\mix, personalMixes, \mul, masterVolume], g, \addToTail);
				("Created synth" + "JackTripPersonalMixOut" + node.nodeID).postln;
			});

			// signal that the mix has started
			// signal is defined in the BaseMix class and represents a Condition object
			// after these two lines are executed, the BaseMix knows that the
			// proper Synths have been set up, and can execute other routines
			this.mixStarted.test = true;
			this.mixStarted.signal;
		}.run;
	}

	// stop all audio on the server
	// frees all Synth nodes
	stop {
		server.freeAll;
	}

	// display a graphical user interface for mixing controls
	gui { | maxSlidersPerRow = 48, maxMultiplier = 5 |
		var in;
		var out;
		var mix = 1 ! (maxClients * maxClients);
		var pan = 0 ! maxClients;
		var rows = 1;
		var cols = maxClients + 1;
		var window;
		var master;
		var sliders = Array.newClear(maxClients);
		var panKnobs = Array.newClear(maxClients);
		var serverInput;
		var connectButton;
		var x = 0;
		var y = 0;

		// run this when connect button is clicked
		var connectToServer = Routine {
			// update serverIp and connect to remote server
			serverIp = serverInput.string.stripWhiteSpace;
			this.connect;
			serverReady.wait;

			defer {
				// initialize levels to 1.0
				in = ParGroup.basicNew(server, 100);
				in.set(\mul, 1);
				out = ParGroup.basicNew(server, 200);
				out.set(\mul, 1);
				master.value = 1.0 / maxMultiplier;
				maxClients.do({ arg n;
					sliders[n].value = 1.0 / maxMultiplier;
				});
			};
		};

		// prepare slider grid
		if (maxClients >= maxSlidersPerRow, {
			rows = (maxClients + 1 / maxSlidersPerRow).roundUp;
			cols = maxSlidersPerRow;
		});

		// create a new window
		window = Window.new("JackTrip AutoPan Mixer", Rect(50,50,(50*cols)+30,(300*rows)+60));

		// add master slider
		master = Slider.new(window, Rect(20,80,40,200));
		master.background = "black";
		master.action_( { arg me;
			var mul = me.value * maxMultiplier;
			("master vol ="+mul).postln;
			out.set(\mul, mul);
		});
		StaticText(window, Rect(20, 280, 40, 20)).string_("Master");

		// add controls for each client
		maxClients.do({ arg n;
			if ((x+1) < cols, { x = x + 1; }, { x = 0; y = y + 1; });

			sliders[n] = Slider.new(window, Rect(20+(x*50), 80+(300*y), 40, 200)).action_( { arg me;
				var mul = me.value * maxMultiplier;
				("ch"+n+"vol ="+mul).postln;
				// for now, just set same value for each pos
				// this ensures it works regardless of whether personal mixes are being used, or not
				maxClients.do{ arg i;
					mix[(maxClients * i) + n] = mul;
				};
				out.set(\mix, mix)
			});

			StaticText(window, Rect(30+(x*50), 280+(300*y), 40, 20)).string_(n);

			panKnobs[n] = Knob.new(window, Rect(20+(x*50), 305+(300*y), 40, 40)).action_( { arg me;
				var p = LinLin.kr(me.value, 0, 1, -1, 1);
				("ch"+n+"pan ="+p).postln;
				pan[n] = p;
				in.set(\pan, pan)
			});
		});

		// add input box for server and connect button
		serverInput = TextField(window, Rect(20,20,300,40)).value_(serverIp);
		connectButton = Button(window, Rect(340,20,100,40));
		connectButton.states_([["Connect"]]);
		connectButton.action_(connectToServer);

		// display the window
		window.front;
	}
}