/*
 * SquashToMonoLink: Squashes a signal to mono audio.
 *
 * Requires an array of audio signals or an array of arrays of audio signals.
 *
 * \nested: if false, does a simple sum over the array. Should be false if the input
 *      signal is a simple array. Should be true if the input signal is an array of arrays.
 *      In this case, the transformation applies to each individual subarray.
 * \keepChannels: if true, maintains the number of channels. For example, let
 *      squashing [a, b] and sum = a + b and suppose non-nested arrays. 
 *      If keepChannels is true, the output is [sum, sum].
 *      If false, the output is just sum.
 */
SquashToMonoLink : Link {

    var <> nested;
    var <> keepChannels;

	*new { |nested=false, keepChannels=true|
		^super.new().nested_(nested).keepChannels_(keepChannels);
	}

    transform { |input|
        var signal = input;

        if (nested, {

            // If nested, apply the operation over every subarray
            signal = signal.collect({ arg item;
                var sum;
                var size = item.size;
                if (keepChannels, {
                    sum = Mix(item) ! size;
                }, {
                    sum = Mix(item);
                });

                // Normalize
                sum = MulAdd(sum, 1 / size, 0);
            });
        }, {

            // If not nested, treat the input as a simple array of signals
            var size = signal.size;
            if (keepChannels, {
                signal = Mix(signal) ! size;
            }, {
                signal = Mix(signal);
            });

            // Normalize
            signal = MulAdd(signal, 1 / size, 0);
        });

    // Return the output signal
    ^signal;
    }
}