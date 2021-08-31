/*
 * JackTripSimpleMix: a minimal mix that scales well
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \inputChannelsPerClient: number of input channels received from each client
 * \outputChannelsPerClient: number of output channels sent to each client
 * \withJamulus: create mixes adapted Jamulus being connected on channels 1 & 2
 * \mix : array of amplitude level multipliers (default 1.0) for each client
 * \mul : master amplitude level multiplier (default 1.0)
 */
~jacktripSimpleMix = { | maxClients, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false |
    var levels = \mix.kr([1 ! maxClients]) * \mul.kr(1.0);
    if (withJamulus, {
        var jackTripSignal = JackTripInput(maxClients - 1, inputChannelsPerClient, true, inputChannelsPerClient).getSignal();
        var jamulusSignal = JackTripInput(1, inputChannelsPerClient, true, 0).getSignal();
        ~aggregateAndSendToAll.value(jackTripSignal, levels, maxClients, outputChannelsPerClient);
        ~sendFirstToEveryoneElse.value(jamulusSignal, levels, maxClients, outputChannelsPerClient);
    }, {
        var jackTripSignal = JackTripInput(maxClients, inputChannelsPerClient).getSignal();
        ~aggregateAndSendToAll.value(jackTripSignal, levels, maxClients, outputChannelsPerClient);
    });
};
