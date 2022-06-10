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
 * CompressorLink: applies downward compression to an audio signal if the
 * specified ratio is between 0 and 1.
 *
 * \thresh: amplitude trigger threshold, in decibels (< 0)
 * \attack: how quickly compression is applied after amplitude exceeds threshold (in seconds)
 * \release: how quickly compression is released after amplitude drops below threshold (in seconds)
 * \ratio: indicates how much compression is applied [0, 1]; default 0.5 = 1:2 compression
 */

CompressorLink : Link {
    var<> thresh;
    var<> attack;
    var<> release;
    var<> ratio;

    *new { | thresh = -10, attack = 0.01, release = 0.02, ratio = 0.5 |
        ^super.new().thresh_(thresh).attack_(attack).release_(release).ratio_(ratio);
    }

    ar { | input, id = "" |
        var signal = input;
        signal = Compander.ar(signal, signal,
            thresh:     \compressor_thresh.kr(thresh).dbamp,    // amplitude trigger threshold [-1, 1]
            clampTime:  \compressor_attack.kr(attack),          // time (in seconds) before compression is applied
            relaxTime:  \compressor_release.kr(release),        // time (in seconds) before compression is removed
            slopeBelow: 1,                                      // range if gate; otherwise, 1
            slopeAbove: \compressor_ratio.kr(ratio)             // ratio if compression; otherwise, 1
        );
        ^signal
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        ^[\compressor_thresh, thresh, \compressor_attack, attack, \compressor_release, release, \compressor_ratio, ratio];
    }
}
