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
		^super.new(maxClients).autopan_(autopan).panSlots_(panSlots).hpf_(hpf).lpf_(lpf);
	}

	// sendSynthDefs method sends definitions to the server for use in audio mixing
	// Note that SynthDef.send() compiles the SynthDef and sends it to the server
	sendSynthDefs {
		// default master mix does nothing
		var defaultMix = 1 ! maxClients;

		// allocate a stereo audio bus the handle input from each client
		// the first maxClients * outputChannelsPerClient channels are reserved for audio outputs
		// the subsequent maxClients * inputChannelsPerClient channels are reserved for audio inputs
		// audio is read from the audio input channels and is sent back to each client through the
		// audio output channels
		// private buses are reserved for internally routing audio signals
		var firstPrivateBus = (maxClients * (inputChannelsPerClient + outputChannelsPerClient));
		inputBuses = Array.fill(maxClients, { |n|
			var i = firstPrivateBus + (n * inputChannelsPerClient);
			Bus.new('audio', i, inputChannelsPerClient, server);
		});

		/*
		 * jacktrip_simple_in is used to apply leveling and filters to a specific client input, 
		 * and send it to an audio bus
		 *
		 * \low : low frequency to use for bandpass filter (default 20)
		 * \high : high frequency to use for bandpass filter (default 20000)
		 * \mul : amplitude level multiplier (default 1.0)
		 */
		"Sending SynthDef: jacktrip_simple_in".postln;
		SynthDef("jacktrip_simple_in", {
			var signal, out;

			signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
			signal = BandPassFilterLink(\low.kr(20), \high.kr(20000)).transform(signal);
			signal = MultiplyLink(masterVolume * \mul.kr(1)).transform(signal);

			signal.do({ arg item, i;
				Out.ar(inputBuses[i].index, item);
			});
		}).send(server);

		/*
		 * jacktrip_panned_in is used to apply leveling, filters and panning to a specific client input, 
		 * and send it to an audio bus
		 *
		 * \low : frequency to use for lowpass filter (default 20)
		 * \high : frequency to use for bandpass filter (default 20000)
		 * \mul : amplitude level multiplier (default 1.0)
		 */

		"Sending SynthDef: jacktrip_panned_in".postln;
		SynthDef("jacktrip_panned_in", {
			var signal, out;

			var pSlots = panSlots;
			var panValues;
			var p;

			// automatically pan clients across stereo field
			if (pSlots > maxClients, { pSlots = maxClients; });
			if (pSlots < 2, {
				panValues = [0];
				pSlots = 1;
			}, {
				// LinLin maps a range of input values linearly to a range of
				// output values
				panValues = Array.fill(pSlots, { arg i;
					LinLin.kr((i % pSlots) + 1, 0, pSlots + 1, -1, 1);
				});
			});

			p = Array.fill(maxClients, { arg i;
				panValues[i % pSlots];
			});

			signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
			signal = BandPassFilterLink(\low.kr(20), \high.kr(20000)).transform(signal);
			signal = SquashToMonoLink(true, false).transform(signal);
			signal = PanningLink(p).transform(signal);
			signal = MultiplyLink(masterVolume * \mul.kr(1)).transform(signal);

			signal.do({ arg item, i;
				Out.ar(inputBuses[i].index, item);
			});
		}).send(server);

		/*
		 * jacktrip_personalmix_out is used to create a personal mix by combining output from the input buses
		 *
		 * \mul : amplitude level multiplier (default 1.0)
		 */
		SynthDef("jacktrip_personalmix_out", {
			maxClients.do({ arg clientNum;
				var signal, mix;

				mix = defaultMix;
				if (clientNum == 0, {
					mix[clientNum] = 0;
				}, {
					mix[clientNum] = selfVolume;
				});

				signal = JackTripInput(maxClients, outputChannelsPerClient, false, firstPrivateBus).getSignal();
				signal = MultiplyLink(mix).transform(signal);
				signal = AggregateLink().transform(signal);
				signal = MultiplyLink(masterVolume * \mul.kr(1)).transform(signal);
				Out.ar(outputChannelsPerClient * clientNum, signal);
			});
		}).send(server);

		/*
		 * jamulus_simple_out is used to create a unique mix for output to jamulus bridge
		 *
		 * \mul : amplitude level multiplier (default 1.0)
		 */
		"Sending SynthDef: jamulus_simple_out".postln;
		SynthDef("jamulus_simple_out", {
			var in, signal, volumeOpt, mix;

			mix = defaultMix;
			mix[0] = 0;

			signal = JackTripInput(maxClients, outputChannelsPerClient, false, firstPrivateBus).getSignal();
			signal = MultiplyLink(mix).transform(signal);
			signal = AggregateLink().transform(signal);
			signal = MultiplyLink(\mul.kr(1)).transform(signal);
			Out.ar(0, signal);

		}).send(server);

		/*
		 * jacktrip_simple_out is used to create a single master mix for jacktrip client output
		 *
		 * \mul : amplitude level multiplier (default 1.0)
		 */
		"Sending SynthDef: jacktrip_simple_out".postln;
		SynthDef("jacktrip_simple_out", {
			// exclude sending to jamulus on channel 0 (handled by jamulus_simple_out)
			var signal, out;

			signal = JackTripInput(maxClients, outputChannelsPerClient, false, firstPrivateBus).getSignal();
			signal = AggregateLink().transform(signal);
			signal = MultiplyLink(\mul.kr(1)).transform(signal);

			out = Array.fill(maxClients - 1, { | clientNum |
				(clientNum + 1) * outputChannelsPerClient;
			});

			Out.ar(out, signal);

		}).send(server);
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
			this.sendSynthDefs.value;
			
			// use group 100 for client input synths and use group 200 for client output synths
			// p_new is a server command (see Server Command Reference on SC documentation)
			// that creates a parallel group, which represents a set of Synths that execute
			// simultaneously
			server.sendMsg("/p_new", 100, 1, 0);
			server.sendMsg("/p_new", 200, 1, 0);

			// wait for server to receive synthdefs
			server.sync;

			if (autopan, {
				var node;
				
				// Squash the clients tracks to mono, then pan them before they reach
				// the input buses.
				("automatically panning clients across").postln;
				node = Synth("jacktrip_panned_in", [\low, hpf, \high, lpf], g, \addToTail);
				("Created synth" + "jacktrip_panned_in" + node.nodeID).postln;

			}, {
				// do not pan clients and do not squash to mono.
				var node;
				node = Synth("jacktrip_simple_in", [\low, hpf, \high, lpf], g, \addToTail);
				("Created synth" + "jacktrip_simple_in" + node.nodeID).postln;
			});

			g = 200;

			// scsynth starts to max out a core after about 100 personal mixes,
			// and supernova throws mysterous bad_alloc errors
			if (maxClients > 100, {
				var node;

				// create unique output for jamulus that excludes itself
				// only outputs to jamulus
				node = Synth("jamulus_simple_out", [], g, \addToTail);
				("Created synth" + "jamulus_simple_out" + node.nodeID).postln;

				// create output for all jacktrip clients that includes jamulus
				// outputs to all clients except jamulus
				node = Synth("jacktrip_simple_out", [], g, \addToTail);
				("Created synth" + "jacktrip_simple_out " + node.nodeID).postln;
			}, {

				var node;

				// create personal mix for all jacktrip clients that includes jamulus
				// outputs to all clients including jamulus
				node = Synth("jacktrip_personalmix_out", [], g, \addToTail);
				("Created synth" + "jacktrip_personalmix_out" + node.nodeID).postln;
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
	gui { | maxSlidersPerRow = 48, maxMultiplier = 5, startNode = 1000 |
		var in;
		var out;
		var mix = 1 ! maxClients;
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
					var node = Node.basicNew(server, startNode + n);
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
				server.sendMsg("/n_set", startNode + n, \mul, mul);
			});

			StaticText(window, Rect(30+(x*50), 280+(300*y), 40, 20)).string_(n);

			panKnobs[n] = Knob.new(window, Rect(20+(x*50), 305+(300*y), 40, 40)).action_( { arg me;
				var p = LinLin.kr(me.value, 0, 1, -1, 1);
				("ch"+n+"pan ="+p).postln;
				server.sendMsg("/n_set", startNode + n, \pan, p);
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
