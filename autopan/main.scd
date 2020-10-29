/*
 * autopan: automatically pans clients across a stereo sound field
 *
 * ~panSlots: number of panning slots to use
 */ 

// default personal mix does nothing
~defaultMix = [1 ! ~maxClients];

// allocate a stereo audio bus the handle input from each client
// work-around: SC ignores input and output channels when calculating private bus numbers (it assumes only 2)
~firstPrivateBus = (~maxClients * (~inputChannelsPerClient + ~outputChannelsPerClient));
~inputBuses = Array.fill(~maxClients, { |n|
	var i = ~firstPrivateBus + (n * ~inputChannelsPerClient);
	Bus.new('audio', i, ~inputChannelsPerClient, s);
});

// sendSynthDefs sends definitions to the server for use in audio mixing
~sendSynthDefs = {
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
		var mono = Mix.fill(~inputChannelsPerClient, { arg channelNum;
			var in = SoundIn.ar((client*~inputChannelsPerClient)+channelNum);
			in = LPF.ar(in, \lpf.kr(20000));
			HPF.ar(in, \hpf.kr(20));
		});

		// pan mono across stereo field
		var panned = Pan2.ar(mono, \pan.kr(0));

		// send sound to output bus
		Out.ar(\out.ir(0), panned * \mul.kr(1));
	}).send(s);

	/*
	 * jacktrip_personalmix_out is used to create a personal mix by combining output from the input buses
	 *
	 * \client : client number to use for bus & output channel offsets
	 * \mix : array of levels used for output mix (default [1 ! ~maxClients])
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	"Sending SynthDef: jacktrip_personalmix_out".postln;
	SynthDef("jacktrip_personalmix_out", {
		var in = Mix.fill(~maxClients, { arg n;
			var b = ~inputBuses[n];
			In.ar(b, ~inputChannelsPerClient) * \mix.kr(~defaultMix)[n];
		});
		Out.ar(~outputChannelsPerClient * \client.ir(0), in * \mul.kr(1));
	}).send(s);

	/*
	 * jamulus_autopan_out is used to create a unique mix for output to jamulus bridge
	 *
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	"Sending SynthDef: jamulus_autopan_out".postln;
	SynthDef("jamulus_autopan_out", {
		// exclude jamulus input from the mix sent back to jamulus
		var in = Mix.fill(~maxClients - 1, { arg n;
			var b = ~inputBuses[n + 1];
			In.ar(b, ~inputChannelsPerClient);
		});
		// send only to jamulus on channel 0
		Out.ar(0, in * \mul.kr(1));
	}).send(s);

	/*
	 * jamulus_autopan_out is used to create a single master mix for jacktrip client output
	 *
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	"Sending SynthDef: jacktrip_autopan_out".postln;
	SynthDef("jacktrip_autopan_out", {
		// exclude sending to jamulus on channel 0 (handled by jamulus_autopan_out)
		var in = Mix.fill(~maxClients, { arg n;
			In.ar(~inputBuses[n], ~inputChannelsPerClient);
		});
		var out = Array.fill(~maxClients - 1, { | clientNum |
			(clientNum + 1) * ~outputChannelsPerClient;
		});
		Out.ar(out, in * \mul.kr(1));
	}).send(s);

	/*
	 * tish is just a nice sounding SC example for testing and possibly on-demand metronome
	 *
	 * \freq : frequency to use for unit generator
	 * \rate : impluse rate
	 * \out : output bus to use for sending audio (default 0)
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	"Sending SynthDef: tish".postln;
	SynthDef("tish", { arg freq = 1200, rate = 2, out = 0, mul = 1;
		var osc, trg;
		trg = Decay2.ar(Impulse.ar(rate,0,0.3), 0.01, 0.3);
		osc = {WhiteNoise.ar(trg)}.dup;
		Out.ar(out, osc*mul); // send output to audio bus zero.
	}).send(s);
};

// ~startSynths starts up all the audio on the server
~startSynths = {
	var g = 100;
	var panSlots = ~panSlots;
	var panValues;

	if (panSlots > ~maxClients, { panSlots = ~maxClients; });
	if (panSlots < 2, {
		panValues = [0];
		panSlots = 1;
	}, {
		panValues = Array.fill(panSlots, { arg i;
			LinLin.kr((i % panSlots) + 1, 0, panSlots + 1, -1, 1);
		});
	});
	("automatically panning clients across" + panSlots + "slots").postln;

	// use group 100 for client input synths
	s.sendMsg("/p_new", g, 1, 0);

	// start client input synths
	~maxClients.do { | clientNum |
		var b = ~inputBuses[clientNum];
		var p = panValues[clientNum % panSlots];
		var node = Synth("jacktrip_autopan_in", [\client, clientNum, \out, b, \pan, p], g, \addToTail);
		("Created synth" + "jacktrip_autopan_in" + node.nodeID + "on bus" + b.index + "pan" + p).postln;
	};

	// use group 200 for client output synths
	g = 200;
	n = 2000;
	s.sendMsg("/p_new", g, 1, 0);

	// scsynth maxes out a single core after about 100 personal mixes,
	// and supernova throws mysterous bad_alloc errors
	if (~maxClients > 100, {
		var node;

		// create unique output for jamulus that excludes itself
		node = Synth("jamulus_autopan_out", [], g, \addToTail);
		("Created synth" + "jamulus_autopan_out" + node.nodeID).postln;

		// create output for all jacktrip clients that includes jamulus
		node = Synth("jacktrip_autopan_out", [], g, \addToTail);
		("Created synth" + "jacktrip_autopan_out " + node.nodeID).postln;
	}, {
		// create a unique output synth for each client to handle personal mixes
		~maxClients.do { | clientNum |
			var mix = ~defaultMix;
			var node;

			if (clientNum == 0, {
				// create a unique mix for jamulus that excludes itself
				mix = Array.newFrom(~defaultMix);
				mix[0] = 0;
			});

			node = Synth("jacktrip_personalmix_out", [\client, clientNum, \mix: mix], g, \addToTail);
			("Created synth jacktrip_personalmix_out" + node.nodeID).postln;
		};
	});
};

// ~main is called automatically via sclang service
~main = Routine {
	~serverReady.wait;
	s.freeAll;
	~sendSynthDefs.value;
	s.sync;
	~startSynths.value;
};
