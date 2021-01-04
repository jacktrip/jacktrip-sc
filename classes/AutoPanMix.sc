/*
 * AutoPanMix: automatically pans clients across a stereo sound field,
 *             and supports personal mixes up to 100 clients
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \panSlots: number of panning slots to use across a stereo sound field
 * \selfVolume: default volume level multiplier used for yourself in your personal mix, when enabled
 */ 
AutoPanMix : BaseMix {
	var <>panSlots, <>selfVolume;
	var inputBuses;

	// create a new instance
	*new { | maxClients = 16, panSlots = 3, selfVolume = 1.0 |
		^super.new(maxClients).panSlots_(panSlots);
	}

	// sendSynthDefs method sends definitions to the server for use in audio mixing
	sendSynthDefs {
		// default master mix does nothing
		var defaultMix = 1 ! maxClients;

		// allocate a stereo audio bus the handle input from each client
		// work-around: SC ignores input and output channels when calculating private bus numbers (it assumes only 2)
		var firstPrivateBus = (maxClients * (inputChannelsPerClient + outputChannelsPerClient));
		inputBuses = Array.fill(maxClients, { |n|
			var i = firstPrivateBus + (n * inputChannelsPerClient);
			Bus.new('audio', i, inputChannelsPerClient, server);
		});

		/*
		* jacktrip_autopan_in is used to apply leveling, filters and panning to a specific client input, and send it to an audio bus
		*
		* \client : client number to use for bus & input channel offsets
		* \lpf : frequency to use for low pass filter (default 20000)
		* \hpf : frequency to use for high pass filter (default 20)
		* \pan : pan position from -1 left to 1 right (default 0)
		* \out : output bus to use for sending audio (default 0)
		* \mul : amplitude level multiplier (default 1.0)
		*/
		"Sending SynthDef: jacktrip_autopan_in".postln;
		SynthDef("jacktrip_autopan_in", {
			var client = \client.ir(0);

			// squash input channels into mono
			var mono = Mix.fill(inputChannelsPerClient, { arg channelNum;
				var in = SoundIn.ar((client*inputChannelsPerClient)+channelNum);
				in = LPF.ar(in, \lpf.kr(20000));
				HPF.ar(in, \hpf.kr(20));
			});

			// pan mono across stereo field
			var panned = Pan2.ar(mono, \pan.kr(0));

			// send sound to output bus
			Out.ar(\out.ir(0), panned * masterVolume * \mul.kr(1));
		}).send(server);

		/*
		* jacktrip_personalmix_out is used to create a personal mix by combining output from the input buses
		*
		* \client : client number to use for bus & output channel offsets
		* \mix : array of levels used for output mix (default [1 ! maxClients])
		* \mul : amplitude level multiplier (default 1.0)
		*/
		"Sending SynthDef: jacktrip_personalmix_out".postln;
		SynthDef("jacktrip_personalmix_out", {
			var in = Mix.fill(maxClients, { arg n;
				var b = inputBuses[n];
				In.ar(b, inputChannelsPerClient) * \mix.kr(defaultMix)[n];
			});
			Out.ar(outputChannelsPerClient * \client.ir(0), in * \mul.kr(1));
		}).send(server);

		/*
		* jamulus_autopan_out is used to create a unique mix for output to jamulus bridge
		*
		* \mul : amplitude level multiplier (default 1.0)
		*/
		"Sending SynthDef: jamulus_autopan_out".postln;
		SynthDef("jamulus_autopan_out", {
			// always exclude jamulus input from the mix sent back to jamulus
			var in = Mix.fill(maxClients - 1, { arg n;
				var b = inputBuses[n + 1];
				In.ar(b, inputChannelsPerClient);
			});
			// send only to jamulus on channel 0
			Out.ar(0, in * \mul.kr(1));
		}).send(server);

		/*
		* jamulus_autopan_out is used to create a single master mix for jacktrip client output
		*
		* \mul : amplitude level multiplier (default 1.0)
		*/
		"Sending SynthDef: jacktrip_autopan_out".postln;
		SynthDef("jacktrip_autopan_out", {
			// exclude sending to jamulus on channel 0 (handled by jamulus_autopan_out)
			var in = Mix.fill(maxClients, { arg n;
				In.ar(inputBuses[n], inputChannelsPerClient);
			});
			var out = Array.fill(maxClients - 1, { | clientNum |
				(clientNum + 1) * outputChannelsPerClient;
			});
			Out.ar(out, in * \mul.kr(1));
		}).send(server);
	}

	// starts up all the audio on the server
	start {
		var g = 100;
		var pSlots = panSlots;
		var panValues;

		Routine {
			// wait for server to be ready
			serverReady.wait;

			// free any existing nodes
			server.freeAll;

			// send synthDefs
			this.sendSynthDefs.value;
			
			// use group 100 for client input synths
			server.sendMsg("/p_new", 100, 1, 0);

			// use group 200 for client output synths
			server.sendMsg("/p_new", 200, 1, 0);

			// wait for server to receive synthdefs
			server.sync;

			if (pSlots > maxClients, { pSlots = maxClients; });
			if (pSlots < 2, {
				panValues = [0];
				pSlots = 1;
			}, {
				panValues = Array.fill(pSlots, { arg i;
					LinLin.kr((i % pSlots) + 1, 0, pSlots + 1, -1, 1);
				});
			});
			("automatically panning clients across" + pSlots + "slots").postln;

			// start client input synths
			maxClients.do { | clientNum |
				var b = inputBuses[clientNum];
				var p = panValues[clientNum % pSlots];
				var node = Synth("jacktrip_autopan_in", [\client, clientNum, \out, b, \pan, p], g, \addToTail);
				("Created synth" + "jacktrip_autopan_in" + node.nodeID + "on bus" + b.index + "pan" + p).postln;
			};

			g = 200;

			// scsynth starts to max out a core after about 100 personal mixes,
			// and supernova throws mysterous bad_alloc errors
			if (maxClients > 100, {
				var node;

				// create unique output for jamulus that excludes itself
				node = Synth("jamulus_autopan_out", [], g, \addToTail);
				("Created synth" + "jamulus_autopan_out" + node.nodeID).postln;

				// create output for all jacktrip clients that includes jamulus
				node = Synth("jacktrip_autopan_out", [], g, \addToTail);
				("Created synth" + "jacktrip_autopan_out " + node.nodeID).postln;
			}, {
				// create a unique output synth for each client to handle personal mixes
				maxClients.do { | clientNum |
					var mix = 1 ! maxClients;
					var node;

					if (clientNum == 0, {
						// create a unique mix for jamulus that excludes itself
						mix[0] = 0;
					}, {
						mix[clientNum] = selfVolume;
					});

					node = Synth("jacktrip_personalmix_out", [\client, clientNum, \mix: mix], g, \addToTail);
					("Created synth jacktrip_personalmix_out" + node.nodeID).postln;
				};
			});

			// signal that the mix has started
			this.mixStarted.test = true;
			this.mixStarted.signal;
		}.run;
	}

	// stop all audio on the server
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
