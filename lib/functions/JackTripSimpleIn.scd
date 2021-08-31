/*
 * JackTripSimpleIn is used to apply leveling and filters to a specific client input, 
 * and send it to an audio bus
 *
 * \low : low frequency to use for bandpass filter (default 20)
 * \high : high frequency to use for bandpass filter (default 20000)
 * \mul : amplitude level multiplier (default 1.0)
 */
~jacktripSimpleIn = { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
    var signal;

    signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
    signal = BandPassFilterLink(\low.kr(20), \high.kr(20000)).transform(signal);
    signal = MultiplyLink(\mul.kr(1)).transform(signal);

    signal.do({ arg item, i;
        Out.ar(~inputBuses[i].index, item);
    });
}