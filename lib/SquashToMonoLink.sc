SquashToMonoLink : Link {

    var <> nested;
    var <> keepChannels;

	*new { |nested=false, keepChannels=true|
		^super.new().nested_(nested).keepChannels_(keepChannels);
	}

    transform { |input|
        var signal = input;

        if (nested, {
            signal = signal.collect({ arg item;
                var sum;
                var size = item.size;
                if (keepChannels, {
                    sum = Mix(item) ! size;
                }, {
                    sum = Mix(item);
                });
                sum = MulAdd(sum, 1 / size, 0);
                sum
            });
        }, {
            var size = signal.size;
            if (keepChannels, {
                signal = Mix(signal) ! size;
            }, {
                signal = Mix(signal);
            });

            signal = MulAdd(signal, 1 / size, 0);
        });
    ^signal;
    }
}