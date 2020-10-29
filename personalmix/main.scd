/*
 * personalmix: allows each client to create a personal mix
 *
 * ~panSlots: number of panning slots to use
 */ 

// default personal mix does nothing
~defaultMix = [1 ! ~maxClients];

// sendSynthDefs sends definitions to the server for use in audio mixing
~sendSynthDefs = {
	/*
	 * jacktrip_personalmix_out is used to create a personal mix by combining output from the input buses
	 *
	 * \client : client number to use for bus & output channel offsets
	 * \lpf : frequency to use for low pass filter (default 20000)
	 * \hpf : frequency to use for high pass filter (default 20)
	 * \mix : array of levels used for output mix (default [1 ! ~maxClients])
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	"Sending SynthDef: jacktrip_personalmix_out".postln;
	SynthDef("jacktrip_personalmix_out", {
		var client = \client.ir(0);
		var in = Mix.fill(~maxClients, { arg clientNum;
			Array.fill(~inputChannelsPerClient, { arg n;
				var in = SoundIn.ar((clientNum*~inputChannelsPerClient)+n);
				in = LPF.ar(in, \lpf.kr(20000));
				HPF.ar(in, \hpf.kr(20));
			}) * \mix.kr(~defaultMix)[clientNum];
		});
		Out.ar(~outputChannelsPerClient * client, in * \mul.kr(1));
	}).send(s);
};

// ~startSynths starts up all the audio on the server
~startSynths = {
	var node;
	var g = 200;	// use group 200 for client output synths
	s.sendMsg("/p_new", g, 1, 0);

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
};

// ~main is called automatically via sclang service
~main = Routine {
	~serverReady.wait;
	s.freeAll;
	~sendSynthDefs.value;
	s.sync;
	~startSynths.value;
};
