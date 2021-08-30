/* 
 * PanningLink: pans a signal across a stereo field.
 *
 * If the input signal and the pan value are arrays of the same dimension, then
 * multichannel expansion will be invoked and each channel will be panned according
 * the corresponding value in the pan array.
 */
PanningLink : Link {
    var<> pan;

	*new { | pan=0 |
		^super.new().pan_(pan);
	}

    transform { |input|
        var signal;
        signal = Pan2.ar(input, pan);
        ^signal;
    }

    // returns an array of initial pan values for each client, from -1 to 1
    *autoPan { | maxClients, slots |
        var panValues;

        // automatically pan clients across stereo field
        if (slots > maxClients, { slots = maxClients; });
        if (slots < 2, {
            panValues = [0];
            slots = 1;
        }, {
            // LinLin maps a range of input values linearly to a range of
            // output values
            panValues = Array.fill(slots, { arg i;
                LinLin.kr((i % slots) + 1, 0, slots + 1, -1, 1);
            });
        });

        ("panning" + maxClients + "clients across" + slots + "slots:" + panValues).postln;

        ^Array.fill(maxClients, { arg i;
            panValues[i % slots];
        });
    }
}