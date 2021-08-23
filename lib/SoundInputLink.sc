SoundInputLink : Class {

    var<> numClients;
    var<> inputChannelsPerClient;

    *new { |numClients, inputChannelsPerClient|
		^super.new().numClients_(numClients).inputChannelsPerClient_(inputChannelsPerClient);
	}

    getSignal {
        ^Array.fill(numClients, { arg clientNum;
            var offset = clientNum * inputChannelsPerClient;
            Array.fill(inputChannelsPerClient, { arg ch;
                SoundIn.ar(offset + ch);
            });
        });
    }
}