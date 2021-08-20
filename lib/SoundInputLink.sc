SoundInputLink : Class {

    var<> numClients;
    var<> inputChannelsPerClient;

    *new { |clients, inputChannelsPerClient|
		^super.new().clients_(clients).inputChannelsPerClient_(inputChannelsPerClient);
	}

    getSignal {
        ^Array.fill(numClients, { arg clientNum;
            Array.fill(inputChannelsPerClient, { arg ch;
                SoundIn.ar(ch);
            });
        });
    }
}