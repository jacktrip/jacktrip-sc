/*
 * JackTripInput: Reads inputs given the number of clients and the number
 * of input channels per client.
 *
 * \numClients: the number of clients to read from
 * \inputChannelsPerClient: the number of channels per client
 * \useSoundIn: if true, uses the SoundIn UGen rather than the In Ugen, 
 *      which automatically accounts for input channel offset for audio inputs.
 * \offset: the offset channel number. If reading from hardware audio input buses,
 *      this should be set to 0 and useSoundIn should be set to true. In general,
 *      this should only be nonzero when useSoundIn is false.
 * 
 * The signal returned is a nested array of UGens, where the first dimension
 * pertains to each client and the second dimension pertains to the number of channels per
 * client. For example, if we want to read audio from 3 clients each with stereo audio input,
 * the resulting signal would look like:
 *
 * [
 *      [Client1_Channel1, Client1_Channel2],
 *      [Client2_Channel1, Client2_Channel2],
 *      [Client3_Channel1, Client3_Channel2],
 * ]
 *
 * Since these array elements are all UGens, the calling function can take advantage of
 * Supercollider's Multichannel Expansion functionality to process many signals at once.
 */
JackTripInput : Class {

    var<> numClients;
    var<> inputChannelsPerClient;
    var<> useSoundIn;
    var<> offset;

    *new { |numClients, inputChannelsPerClient, useSoundIn=true, offset=0|
		^super.new().numClients_(numClients).inputChannelsPerClient_(inputChannelsPerClient).useSoundIn_(useSoundIn).offset_(offset);
	}

    getSignal {
        ^Array.fill(numClients, { arg clientNum;

            // Offset2 is the internal offset in addition to the offset parameter
            // used for internal accounting purposes
            var offset2 = clientNum * inputChannelsPerClient;

            // Each element in the main array is itself an array of input signals
            Array.fill(inputChannelsPerClient, { arg ch;
                var input;
                if (useSoundIn, {
                    
                    // Account for the hardware input bus offset automatically
                    input = SoundIn.ar(offset2 + ch + offset);
                }, {

                    // Do not account for the hardware input bus offset automatically
                    input = In.ar(offset2 + ch + offset);
                });

                // Return the input signal
                input; 
            });
        });
    }
}