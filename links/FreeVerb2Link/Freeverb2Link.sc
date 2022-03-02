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
 * FreeVerb2Link: two-channel reverb using the FreeVerb2 ugen
 * See https://doc.sccode.org/Classes/FreeVerb2.html
 *
 * \mix: balance between dry (unmodified) and wet (reverb) signal [0, 1]
 * \room: used to represent the size of the room [0, 1]
 * \damp: amount of high frequency dampening for wet signal [0, 1]
 */

FreeVerb2Link : Link {
    var<> mix;
    var<> room;
    var<> damp;

    *new { | mix = 0.25, room = 0.15, damp = 0.5 |
        ^super.new().mix_(mix).room_(room).damp_(damp);
    }

    ar { | input, id = "" |
        var signal = input;
        signal = FreeVerb2.ar(signal[0], signal[1],
            mix: \freeverb2_mix.kr(mix),     // dry/wet balance. range 0..1.
            room: \freeverb2_room.kr(room),  // room size. rage 0..1.
            damp: \freeverb2_damp.kr(damp)   // Reverb HF damp. range 0..1.
        );
        ^signal;
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        ^[\freeverb2_mix, mix, \freeverb2_room, room, \freeverb2_damp, damp];
    }
}