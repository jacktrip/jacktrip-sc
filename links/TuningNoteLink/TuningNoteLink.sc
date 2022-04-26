/* 
 * Copyright 2022 JackTrip Labs, Inc.
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
 * TuningNoteLink: Adds a tuning note to a signal at a specified frequency
 *
 */

TuningNoteLink : Link {
    var<> freq;
    var<> vol;

    *new { | freq=440, vol=0.5 |
        ^super.new().freq_(freq).vol_(vol);
    }

    ar { | input, id = "" |
        var signal = input;
        var sine = SinOsc.ar(\tuning_freq.ar(freq));
        signal = MulAdd(sine, \tuning_vol.ar(vol), signal);
        ^signal;
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        ^[\tuning_freq, freq, \tuning_vol, vol];
    }
}
