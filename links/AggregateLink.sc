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

    ar { |input|
        ^Mix(input);
    }
}