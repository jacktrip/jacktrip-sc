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
 * EqualizerLink: applies a one-band parametric equalizer to the audio signal, e.g. allowing to filter out annoying resonances or boost certain range
 *
 * \freq - center frequency of the bell-shaped curve
 * \gain - amount of decibels the signal is amplified or reduced at the selected frequency
 * \q - quality/width of curve, describing how many frequencies next to the center frequency are affected. Small values = narrower band
 */

EqualizerLink : Link {
    var<> freq;
    var<> gain;
    var<> q;

    *new { | freq=300, gain=0, q=1 |
        ^super.new().freq_(freq).gain_(gain).q_(q);
    }

    ar { | input, id = "" |
        var signal = input;
        signal = BPeakEQ.ar(signal,
        freq:       \equalizer_freq.kr(freq),   // center frequency of band (in Hz)
        gain:       \equalizer_gain.kr(gain),   // gain of that band (can be negative too)
        q:          \equalizer_q.kr(q)          // width of frequency band affected
        );
        ^signal
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        ^[\equalizer_freq, freq, \equalizer_gain, gain, \equalizer_q, q];
    }
}
