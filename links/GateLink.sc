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
 * GateLink: removes noise by compressing, if signal amplitude falls is below threshold
 *
 * \thresh: amplitude trigger threshold, in decibels (< 0)
 * \attack: how quickly compression is released after amplitude exceeds threshold (in seconds)
 * \release: how quickly compression is applied after amplitude drops below threshold (in seconds)
 * \range: indicates how much compression is applied (> 1)
 */

GateLink : Link {
    var<> thresh;
    var<> attack;
    var<> release;
    var<> range;

    *new { | thresh = -60, attack = 0.1, release = 0.01, range = 10 |
        ^super.new().thresh_(thresh).attack_(attack).release_(release).range_(range);
    }

    transform { |input|
        var signal = input;
        signal = Compander.ar(signal, signal,
            thresh:     \gatethresh.kr(thresh).dbamp,   // amplitude trigger threshold [-1, 1]
            clampTime:  \gaterelease.kr(release),       // time (in seconds) before compression is applied
            relaxTime:  \gateattack.kr(attack),         // time (in seconds) before compression is removed
            slopeBelow: \gaterange.kr(range),           // range if gate; otherwise, 1
            slopeAbove: 1                               // ratio if compression; otherwise, 1
        );
        ^signal
    }
}