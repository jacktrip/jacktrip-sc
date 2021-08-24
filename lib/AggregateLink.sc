/*
 * Aggregate Link: Performs a reduction on the input signal.
 *
 * If the signal being transformed is an array of nested arrays,
 * then the sum occurs over the first axis.
 *
 * Example: 
 *
 * [
 *      [a1, a2],
 *      [b1, b2],
 *      [c1, c2],
 * ]
 *
 * Reduces to:
 *
 * [a1 + b1 + c1, a2 + b2 + c2]
 *
 * This can be though of as effectively 'flattening' the array
 * over the vertical axis, summing many stereo tracks into one stereo track. 
 * More information on how this works can be found under the Multichannel
 * Expansion article in the SC docs and in the docs for the Mix class.
 */
AggregateLink : Link {
    *new {
		^super.new();
	}

    transform { |input|
        ^Mix(input);
    }
}