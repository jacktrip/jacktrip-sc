/*
 * sendFirstToEveryoneElse: sends the input audio channels from the first
 * client to all other clients
 *
 * \in: array of input audio channels to aggregate together
 * \levels : array of amplitude level multipliers (default 1.0) for each client
 * \maxClients: maximum number of clients that may connect to the audio server
 * \outputChannelsPerClient: number of output channels sent to each client
 */
 
~sendFirstToEveryoneElse = { | in, levels, maxClients, outputChannelsPerClient = 2 |
	var out;

	// adjust levels
	var signal = MultiplyLink(levels).transform(in);

	// send Jamulus signal to JackTrip outputs
	out = Array.fill(maxClients - 1, { arg n;
		(n + 1) * outputChannelsPerClient;
	});
	Out.ar(out, signal[0]);
};
