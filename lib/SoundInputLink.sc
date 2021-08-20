SoundInputLink : Class {

    var<> numClients;
    var<> inputChannelsPerClient;

    *new { |clients, inputChannelsPerClient|
		^super.new().clients_(clients).inputChannelsPerClient_(inputChannelsPerClient);
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