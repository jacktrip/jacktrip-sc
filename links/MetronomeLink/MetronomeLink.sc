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
 * MetronomeLink: Adds a metronome to a signal
 *
 */

MetronomeLink : Link {
    var<> bpm;
    var<> freq;
    var<> vol;

    *new { | bpm = 90, freq=1200, vol=0.5 |
        ^super.new().bpm_(bpm).freq_(freq).vol_(vol);
    }

    ar { | input, id = "" |
        var signal = input;
        var osc, trg;

        trg = Decay2.ar(Impulse.ar(bpm/60, 0, 0.3), 0.01, 0.3);
        osc = {WhiteNoise.ar(trg)}.dup;

        signal = MulAdd(signal, vol, osc);
        ^signal;
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        ^[\bpm, bpm, \freq, freq, \vol, vol];
    }
}