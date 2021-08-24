InputLink : Class {

    var<> numClients;
    var<> inputChannelsPerClient;
    var<> useSoundIn;
    var<> offset;

    *new { |numClients, inputChannelsPerClient, useSoundIn=true, offset=0|
		^super.new().numClients_(numClients).inputChannelsPerClient_(inputChannelsPerClient).useSoundIn_(useSoundIn).offset_(offset);
	}

    getSignal {
        ^Array.fill(numClients, { arg clientNum;
            var offset2 = clientNum * inputChannelsPerClient;
            Array.fill(inputChannelsPerClient, { arg ch;
                var input;
                if (useSoundIn, {
                    input = SoundIn.ar(offset2 + ch + offset);
                }, {
                    input = In.ar(offset2 + ch + offset);
                });
                input; 
            });
        });
    }
}