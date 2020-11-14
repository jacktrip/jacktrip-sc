/*
 * SimpleMix: a minimal mix that scales well
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \serverIp: IP address or hostname of remote audio server
 * \serverPort: port number of remote audio server
 */
SimpleMix : BaseMix {

	// create a new instance
	*new { | maxClients = 16 |
		^super.new(maxClients);
	}

	// sendSynthDefs method sends definitions to the server for use in audio mixing
	sendSynthDefs {
		// default master mix does nothing
		var defaultMix = 1 ! maxClients;

		/*
		* jamulus_simple_out is used to create a unique mix for output to jamulus bridge
		*
		* \mix : array of levels used for output mix (default [1 ! maxClients])
		* \mul : amplitude level multiplier (default 1.0)
		*/
		"Sending SynthDef: jamulus_simple_out".postln;
		SynthDef("jamulus_simple_out", {
			// exclude jamulus input from the mix sent back to jamulus
			var in = Mix.fill(maxClients - 1, { arg n;
				var offset = (n + 1) * inputChannelsPerClient;
				Array.fill(inputChannelsPerClient, { arg ch;
					SoundIn.ar(offset+ch) * \mix.kr(defaultMix)[n];
				});
			});
			// send only to jamulus on channel 0
			Out.ar(0, in * masterVolume * \mul.kr(1));
		}).send(server);

		/*
		* jamulus_simple_out is used to create a single master mix for jacktrip client output
		*
		* \mix : array of levels used for output mix (default [1 ! maxClients])
		* \mul : amplitude level multiplier (default 1.0)
		*/
		"Sending SynthDef: jacktrip_simple_out".postln;
		SynthDef("jacktrip_simple_out", {
			// mix together all input channels
			var in = Mix.fill(maxClients, { arg n;
				var offset = n * inputChannelsPerClient;
				Array.fill(inputChannelsPerClient, { arg ch;
					SoundIn.ar(offset+ch) * \mix.kr(defaultMix)[n];
				});
			});
			// exclude sending to jamulus on channel 0 (handled by jamulus_simple_out)
			var out = Array.fill(maxClients - 1, { arg n;
				(n + 1) * outputChannelsPerClient;
			});
			Out.ar(out, in * masterVolume * \mul.kr(1));
		}).send(server);
	}

	// starts up all the audio on the server
	start {
		var node;
		var g = 200;	// use group 200 for client output synths
		
		Routine {
			// wait for server to be ready
			serverReady.wait;

			// free any existing nodes
			server.freeAll;

			// send synthDefs
			this.sendSynthDefs.value;

			// create group
			server.sendMsg("/p_new", g, 1, 0);

			// wait for server to receive synthdefs
			server.sync;

			// create unique output for jamulus that excludes itself
			node = Synth("jamulus_simple_out", [], g, \addToTail);
			("Created synth jamulus_simple_out" + node.nodeID).postln;

			// create output for all jacktrip clients that includes jamulus
			node = Synth("jacktrip_simple_out", [], g, \addToTail);
			("Created synth jacktrip_simple_out" + node.nodeID).postln;
		}.run;
	}

	// stop all audio on the server
	stop {
		server.freeAll;
	}

	// display a graphical user interface for mixing controls
	gui { | maxSlidersPerRow = 48, maxMultiplier = 5 |
		var out;
		var mix = 1 ! maxClients;
		var rows = 1;
		var cols = maxClients + 1;
		var window;
		var master;
		var sliders;
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
				out = ParGroup.basicNew(server, 200);
				out.set(\mul, 1);
				out.set(\mix, mix);
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
		window = Window.new("JackTrip Simple Mixer", Rect(50,50,(50*cols)+30,(260*rows)+60));

		// add master slider
		master = Slider.new(window, Rect(20,80,40,200));
		master.background = "black";
		master.action_( { arg me;
			var mul = me.value * maxMultiplier;
			("master vol ="+mul).postln;
			out.set(\mul, mul);
		});
		StaticText(window, Rect(20, 280, 40, 20)).string_("Master");

		// add channel sliders
		sliders = Array.fill(maxClients, { arg n;
			if ((x+1) < cols, { x = x + 1; }, { x = 0; y = y + 1; });
			StaticText(window, Rect(20+(x*50), 280+(260*y), 40, 20)).string_(n);
			Slider.new(window, Rect(20+(x*50), 80+(260*y), 40, 200)).action_( { arg me;
				mix[n] = me.value * maxMultiplier;
				("ch"+n+"vol ="+mix[n]).postln;
				out.set(\mix, mix)
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
