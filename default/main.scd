// default personal mix does nothing
~defaultMix = [1 ! ~clients];

// allocate a stereo audio bus the handle input from each client
// work-around: SC ignores input and output channels when calculating private bus numbers (it assumes only 2)
~firstPrivateBus = (~clients * ~channels_in * 2);
~inputBuses = Array.fill(~clients, { |n|
	var i = ~firstPrivateBus + (n * ~channels_in);
	Bus.new('audio', i, ~channels_in, s);
});

// sendSynthDefs sends definitions to the server for use in audio mixing
~sendSynthDefs = {
	// jacktrip_in is used to apply leveling, filters and panning to a specific client input, and send it to an audio bus
	"Sending SynthDef: jacktrip_in".postln;
	SynthDef("jacktrip_in", {
		var client = \client.ir(0);

		// squash input channels into mono
		var mono = Mix.fill(~channels_in, { arg channelNum;
			var in = SoundIn.ar((client*~channels_in)+channelNum);
			in = LPF.ar(in, \lpf.kr(20000));
			HPF.ar(in, \hpf.kr(20));
		});

		// pan mono across stereo field
		var panned = Pan2.ar(mono, \pan.kr(0));

		// send sound to output bus
		Out.ar(\out.ir(0), panned * \mul.kr(1));
	}).send(s);

	// jacktrip_out is used to create personal mixes
	"Sending SynthDef: jacktrip_out".postln;
	SynthDef("jacktrip_out", {
		var m = Mix.fill(~clients, { arg clientNum;
			var b = ~inputBuses[clientNum];
			In.ar(b, 2) * \mix.kr(~defaultMix)[clientNum];
		});
		Out.ar(~channels_in*\client.ir(0), m* \mul.kr(1));
	}).send(s);

	// jamulus_master_out is used to create a unique mix for output to jamulus bridge
	"Sending SynthDef: jamulus_master_out".postln;
	SynthDef("jamulus_master_out", {
		// exclude jamulus input from the mix sent back to jamulus
		var m = Mix.fill(~clients - 1, { arg clientNum;
			var b = ~inputBuses[clientNum + 1];
			In.ar(b, 2);
		});
		// send only to jamulus on channel 0
		Out.ar(0, m * \mul.kr(1));
	}).send(s);

	// jamulus_master_out is used to create a single master mix for jacktrip client output
	"Sending SynthDef: jacktrip_master_out".postln;
	SynthDef("jacktrip_master_out", {
		// exclude sending to jamulus on channel 0 (handled by jamulus_master_out)
		var channels = Array.fill(~clients - 1, { | clientNum |
			(clientNum + 1) * 2;
		});
		var m = Mix.fill(~clients, { arg n; In.ar(~inputBuses[n], 2); });
		Out.ar(channels, m * \mul.kr(1));
	}).send(s);

	// tish is just a nice sounding SC example for testing and possibly on-demand metronome
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
	var n = 1000;
	var panslots = ~panslots;
	var panvalues;

	if (panslots > ~clients, { panslots = ~clients; });
	if (panslots < 2, {
		panvalues = [0];
		panslots = 1;
	}, {
		panvalues = Array.fill(panslots, { arg i;
			LinLin.kr((i % panslots) + 1, 0, panslots + 1, -1, 1);
		});
	});

	// use group 100 for client input synths
	s.sendMsg("/p_new", g, 1, 0);

	// start client input synths
	~clients.do { | clientNum |
		var b = ~inputBuses[clientNum];
		var p = panvalues[clientNum % panslots];
		Synth("jacktrip_in", [\client, clientNum, \out, b, \pan, p], g, \addToTail);
		//s.sendMsg("/s_new", "jacktrip_in", n, 1, g, \client, clientNum, \out, b);
		("Created synth" + "jacktrip_in " + n + "on bus" + b.index + "pan" + p).postln;
		n = n + 1;
	};

	// use group 200 for client output synths
	g = 200;
	n = 2000;
	s.sendMsg("/p_new", g, 1, 0);

	// scsynth maxes out a single core after about 100 personal mixes
	if (~clients > 100, {
		// create unique output for jamulus that excludes itself
		Synth("jamulus_master_out", [], g, \addToTail);
		("Created synth" + "jamulus_master_out " + n).postln;

		// create output for all jacktrip clients that includes jamulus
		Synth("jacktrip_master_out", [], g, \addToTail);
		("Created synth" + "jacktrip_master_out " + n).postln;
	}, {
		// create a unique output synth for each client to handle personal mixes
		~clients.do { | clientNum |
			var mix = ~defaultMix;
			if (clientNum == 0, {
				// create a unique mix for jamulus that excludes itself
				mix = Array.newFrom(~defaultMix);
				mix[0] = 0;
			});
			Synth("jacktrip_out", [\client, clientNum, \mix: mix], g, \addToTail);
			//s.sendMsg("/s_new", "jacktrip_out", n, 1, g, \client, clientNum);
			("Created synth" + "jacktrip_out " + n).postln;
			n = n + 1;
		};
	});
};

// ~main is called automatically via sclang service
~main = Routine {
	~serverReady.wait;
	s.freeAll;
	~sendSynthDefs.value;
	~startSynths.value;
};
