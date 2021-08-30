/*
 * aggregateAndSendToAll: aggregates an array of input signals, adjust levels,
 * and sends audio to all clients
 *
 * \in: array of input audio channels to aggregate together
 * \levels : array of amplitude level multipliers (default 1.0) for each client
 * \maxClients: maximum number of clients that may connect to the audio server
 * \outputChannelsPerClient: number of output channels sent to each client
 */

~aggregateAndSendToAll = { | in, levels, maxClients, outputChannelsPerClient = 2 |
	var out;

	// adjust levels and aggregate JackTrip signal
	var signal = MultiplyLink(levels).transform(in);
	signal = AggregateLink().transform(signal);

	// send JackTrip signals to all outputs (including Jamulus)
	out = Array.fill(maxClients, { arg n;
		n * outputChannelsPerClient;
	});
	Out.ar(out, signal);
};
