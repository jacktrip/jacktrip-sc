/* 
 * Copyright 2021 JackTrip Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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