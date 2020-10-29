/*
 * simple: a minimal mix that scales well
 */

// sendSynthDefs sends definitions to the server for use in audio mixing
~sendSynthDefs = {
	/*
	 * jamulus_simple_out is used to create a unique mix for output to jamulus bridge
	 *
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	"Sending SynthDef: jamulus_simple_out".postln;
	SynthDef("jamulus_simple_out", {
		// exclude jamulus input from the mix sent back to jamulus
		var in = Mix.fill(~maxClients - 1, { arg n;
			var offset = (n + 1) * ~inputChannelsPerClient;
			Array.fill(~inputChannelsPerClient, { arg ch;
				SoundIn.ar(offset+ch);
			});
		});
		// send only to jamulus on channel 0
		Out.ar(0, in * \mul.kr(1));
	}).send(s);

	/*
	 * jamulus_simple_out is used to create a single master mix for jacktrip client output
	 *
	 * \mul : amplitude level multiplier (default 1.0)
	 */
	"Sending SynthDef: jacktrip_simple_out".postln;
	SynthDef("jacktrip_simple_out", {
		// mix together all input channels
		var in = Mix.fill(~maxClients, { arg n;
			var offset = n * ~inputChannelsPerClient;
			Array.fill(~inputChannelsPerClient, { arg ch;
				SoundIn.ar(offset+ch);
			});
		});
		// exclude sending to jamulus on channel 0 (handled by jamulus_simple_out)
		var out = Array.fill(~maxClients - 1, { arg n;
			(n + 1) * ~outputChannelsPerClient;
		});
		Out.ar(out, in * \mul.kr(1));
	}).send(s);
};

// ~startSynths starts up all the audio on the server
~startSynths = {
	var node;
	var g = 200;	// use group 200 for client output synths
	s.sendMsg("/p_new", g, 1, 0);

	// create unique output for jamulus that excludes itself
	node = Synth("jamulus_simple_out", [], g, \addToTail);
	("Created synth jamulus_simple_out" + node.nodeID).postln;

	// create output for all jacktrip clients that includes jamulus
	node = Synth("jacktrip_simple_out", [], g, \addToTail);
	("Created synth jacktrip_simple_out" + node.nodeID).postln;
};	

// ~main is called automatically via sclang service
~main = Routine {
	~serverReady.wait;
	s.freeAll;
	~sendSynthDefs.value;
	s.sync;
	~startSynths.value;
};
