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
 * MajorChord.scd
 *
 * A boilerplate example for JackTrip Virtual Studio.
 * Paste this code into the 'Advanced' text input box to run
 * this SuperCollider code. Combines all channels into a mono
 * channel, then the resulting signal is played back along with
 * harmonics to make it a major chord. Plays a beep when first
 * executed.
 */


// output to allChannels
// change this array to only send metronome sounds to specific clients
~allChannels = Array.fill(~maxClients, { |n| n * 2; });

// combines input channels, squashes them to mono, and then
// plays them back along with the major chord harmonics
SynthDef("major", {
    var in, sum;
    var thirdUp, fifthUp, octaveUp, octaveDown, sixthDown;

    // seads all channels, combines them into a mono signal
    in = Mix.fill(~maxClients * 2, { arg channelNum;
        var input;
        input = SoundIn.ar(channelNum);
        input = LPF.ar(input, 20000);
        HPF.ar(input, 20);
    });
    
    // shift the signal up and down to make the harmonics of a major chord
    thirdUp = PitchShift.ar(in, pitchRatio: 1.25, mul: 1.5);
    fifthUp = PitchShift.ar(in, pitchRatio: 1.5, mul: 1.5);
    octaveUp = PitchShift.ar(in, pitchRatio: 2, mul: 0.5);
    octaveDown = PitchShift.ar(in, pitchRatio: 0.5);
    sixthDown = PitchShift.ar(in, pitchRatio: 0.75, mul: 1.25);

    // sum the harmonics into one signal
    sum = Mix.ar([in, thirdUp, fifthUp, octaveUp, octaveDown, sixthDown]);
    Out.ar(~allChannels, [sum, sum]);
}).send(s);


// from http://ccrma.stanford.edu/planetccrma/software/supercollider-en/four-beeps.html
// define a beep, then play it once to all channels, then free the synth
// i_* arguments are fixed at instantiation time and cannot be modulated
// the mul ugen input is usually more efficient than *
// doneAction 2 means free the synth node when the envelope has finished
SynthDef("beep", { arg amp=0.5, freq=174.61, i_dur=1.5;
    var env, sine;
    env = Line.kr(1, 0, i_dur, doneAction: 2, mul: amp);
    sine = SinOsc.ar(freq, mul: env);
    Out.ar(~allChannels, [sine, sine]);
}).send(s);

~runAfter = {
    Synth("beep");
    Synth("major");
};

// Uncomment either the "AutoPanMix" line or the "SimpleMix" line based on which you want to use

AutoPanMix(~maxClients).panSlots_(3).selfVolume_(1).masterVolume_(1.0).connect.start.after(~runAfter);
// SimpleMix(~maxClients).masterVolume_(1.0).connect.start.after(~runAfter);