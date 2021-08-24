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
}