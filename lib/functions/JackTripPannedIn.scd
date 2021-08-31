/*
 * JackTripPannedIn is used to apply leveling, filters and panning to a specific client input, 
 * and send it to an audio bus
 *
 * \low : frequency to use for lowpass filter (default 20)
 * \high : frequency to use for bandpass filter (default 20000)
 * \mul : amplitude level multiplier (default 1.0)
 */
~jacktripPannedIn = { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
    var signal;
    var defaultPan = 0 ! maxClients;

    signal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
    signal = BandPassFilterLink(\low.kr(20), \high.kr(20000)).transform(signal);
    signal = SquashToMonoLink(true, false).transform(signal);
    signal = PanningLink(\pan.kr(defaultPan)).transform(signal);
    signal = MultiplyLink(\mul.kr(1)).transform(signal);

    signal.do({ arg item, i;
        Out.ar(~inputBuses[i].index, item);
    });
}