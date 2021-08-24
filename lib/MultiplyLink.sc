/* 
 * MultiplyLink: multiplies a signal by a provided factor.
 *
 * If the input signal and the factor are arrays of the same dimension, then
 * multichannel expansion will be invoked and the factor array can be thought
 * of as an array of weights.
 */
MultiplyLink : Link {
    var<> factor;

	*new { | factor=1.0 |
		^super.new().factor_(factor);
	}

    transform { |input|
        ^MulAdd(input, factor, 0)
    }
}