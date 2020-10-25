~inputBuses = Array.fill(~clients, { Bus.audio(s, 2); });

~sendSynthDefs = {
	"Sending SynthDef: jt_in".postln;
	SynthDef("jt_in", {
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

	"Sending SynthDef: jt_out".postln;
	SynthDef("jt_out", {
		var mix = \mix.kr(1 ! ~clients);
		var m = Mix.fill(~clients, { arg clientNum;
			var b = ~inputBuses[clientNum];
			In.ar(b, 2) * mix[clientNum];
		});
		Out.ar(~channels_in*\client.ir(0), m* \mul.kr(1));
	}).send(s);

	"Sending SynthDef: jt_master_out".postln;
	SynthDef("jt_master_out", {
		var channels = Array.fill(~clients, { | clientNum |
			clientNum * 2;
		});
		var m = Mix.fill(~clients, { arg clientNum;
			var b = ~inputBuses[clientNum];
			In.ar(b, 2);
		});
		Out.ar(channels, m * \mul.kr(1));
	}).send(s);

	"Sending SynthDef: tish".postln;
	SynthDef("tish", { arg freq = 1200, rate = 2, out = 0, mul = 1;
		var osc, trg;
		trg = Decay2.ar(Impulse.ar(rate,0,0.3), 0.01, 0.3);
		osc = {WhiteNoise.ar(trg)}.dup;
		Out.ar(out, osc*mul); // send output to audio bus zero.
	}).send(s);
};

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

	s.sendMsg("/g_new", g, 1, 0);

	~clients.do { | clientNum |
		var b = ~inputBuses[clientNum];
		var p = panvalues[clientNum % panslots];
		Synth("jt_in", [\client, clientNum, \out, b, \pan, p], g, \addToTail);
		//s.sendMsg("/s_new", "jt_in", n, 1, g, \client, clientNum, \out, b);
		("Created synth" + "jt_in " + n + "on bus" + b.index + "pan" + p).postln;
		n = n + 1;
	};

	g = 200;
	n = 2000;
	s.sendMsg("/g_new", g, 1, 0);

	// scsynth maxes out a single core after about 150 personal mixes
	if (~clients > 100, {
		Synth("jt_master_out", [], g, \addToTail);
		("Created synth" + "jt_master_out " + n).postln;
	}, {
		~clients.do { | clientNum |
			Synth("jt_out", [\client, clientNum], g, \addToTail);
			//s.sendMsg("/s_new", "jt_out", n, 1, g, \client, clientNum);
			("Created synth" + "jt_out " + n).postln;
			n = n + 1;
		};
	});
};

~main = {
	s.freeAll;
	~sendSynthDefs.value;
	~startSynths.value;
};
