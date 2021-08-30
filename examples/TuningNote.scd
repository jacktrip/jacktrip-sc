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
 * TuningNote.scd
 *
 * A boilerplate example for JackTrip Virtual Studio.
 * Paste this code into the 'Advanced' text input box to run
 * this SuperCollider code. The signal sent back to the client
 * consists of the tuning note, their own audio, at a lower volume,
 * and the sum of all clients' audio signals, at a lower volume.
 *
 * Change the default frequency (hz) and duration (sec) using the
 * variables below.
 */

~freq = 440;
~len = 30;


// output to allChannels
// change this array to only send metronome sounds to specific clients
~allChannels = Array.fill(~maxClients, { |n| n * 2; });

// takes one client's input and sends it back to that client along with
// a tuning note of a specified frequency
SynthDef("tune", { arg client, amp1=0.45, amp2=0.2, amp3=0.15, freq=440, dur=30;
    var input, env, sum, note, room;

    // generate a sine oscillation for the specified note using an envelope
    env = Line.kr(1, 1, dur, doneAction: 2, mul: amp1);
    note = SinOsc.ar(freq, mul: env);

    // combine the client's two input channels into mono audio
    input = Mix.fill( ~inputChannelsPerClient, { arg n;
        SoundIn.ar(client * ~inputChannelsPerClient + n);
    });
    input = LPF.ar(input, 20000);
    input = HPF.ar(input, 20, mul: amp2);

    // combine the input channels for all clients into mono audio
    room = Mix.fill( ~maxClients * ~inputChannelsPerClient, { arg n;
        SoundIn.ar(n);
    });
    room = LPF.ar(input, 20000);
    room = HPF.ar(room, 20, mul: amp3);

    // sum the signals and play the audio back to the client
    sum = Mix.ar([note, input, room]);
    Out.ar(client * ~outputChannelsPerClient, [sum, sum]);
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

// run this after the mix has started
// start the beep, and generate a "tune" Synth, one per client
~runAfter = {
    Synth("beep");
    ~maxClients.do({ arg clientNum;
        Synth("tune", [\client, clientNum, \freq, ~freq, \dur, ~len]);
    });
};

// Uncomment either the "AutoPanMix" line or the "SimpleMix" line based on which you want to use

AutoPanMix(~maxClients).panSlots_(3).selfVolume_(1).masterVolume_(1.0).connect.start.after(~runAfter);
// SimpleMix(~maxClients).masterVolume_(1.0).connect.start.after(~runAfter);