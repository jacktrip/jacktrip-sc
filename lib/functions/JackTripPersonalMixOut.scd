/*
 * JackTripPersonalMixOut is used to create a personal mix by combining output from the input buses
 *
 * \mul : amplitude level multiplier (default 1.0)
 */
~jacktripPersonalMixOut = { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
    var personalMixes = \mix.kr(1 ! (maxClients * maxClients));

    maxClients.do({ arg clientNum;
        var signal = JackTripInput(maxClients, outputChannelsPerClient, false, ~firstPrivateBus).getSignal();
        var m = Array.fill(maxClients, { arg n;
            personalMixes[(clientNum * maxClients) + n];
        });
    
        var s = MultiplyLink(m * \mul.kr(1)).transform(signal);
        s = AggregateLink().transform(s);
    
        Out.ar(outputChannelsPerClient * clientNum, s);
    });
}