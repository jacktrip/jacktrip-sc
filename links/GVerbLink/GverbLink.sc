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
 * GVerbLink: one-channel reverb using the Gverb ugen
 * See https://doc.sccode.org/Classes/GVerb.html
 *
 * \room: used to represent the size of the room [0, 1]
 * \time: reverb time, in seconds
 * \damp: amount of high frequency damping for wet signal [0, 1]
 * \dampin: amount of high frequency damping for input signal [0, 1]
 * \dry: amount of dry signal (dB)
 * \early: amount of early reflection signal (dB)
 * \tail: amount of tail level (dB)
 */

GVerbLink : Link {
    var<> room;
    var<> time;
    var<> damp;
    var<> dampin;
    var<> dry;
    var<> early;
    var<> tail;

    *new { | room = 10, time = 3, damp = 0.5, dampin = 0.5, dry = 0, early = -3, tail = -6 |
        ^super.new().room_(room).time_(time).damp_(damp).dampin_(dampin).dry_(dry).early_(early).tail_(tail);
    }

    ar { | input, id = "" |
        var signal = input;
        signal = GVerb.ar(signal,
            roomsize: \gverb_room.kr(room),                 // room size, in squared meters
            revtime: \gverb_time.kr(time),                  // reverb time, in seconds.
            damping: \gverb_damp.kr(damp),                  // 0 to 1, high frequency rolloff, 0 damps the reverb signal completely, 1 not at all.
            inputbw: \gverb_dampin.kr(dampin),              // 0 to 1, same as damping control, but on the input signal.
            drylevel: \gverb_dry.kr(dry).dbamp,             // amount of dry signal.
            earlyreflevel: \gverb_early.kr(early).dbamp,    // amount of early reflection level.
            taillevel: \gverb_tail.kr(tail).dbamp           // amount of tail level
        );
        
        //signal = GVerb.ar(signal);
        ^signal;
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        ^[\gverb_room, room, \gverb_time, time, \gverb_damp, damp, \gverb_dampin, dampin, \gverb_dry, dry, \gverb_early, early, \gverb_tail, tail];
    }
}