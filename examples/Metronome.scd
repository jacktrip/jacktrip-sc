/*
 * Metronome.scd
 *
 * A boilerplate metronome example for JackTrip Virtual Studio.
 * Paste this code into the 'Advanced' text input box to run
 * this SuperCollider code. Plays a single beep when first executed,
 * along with the metronome.
 * 
 * Change the default BPM value below to change the metronome rate
 */


// Metronome rate in BPM
~rate = 90;

// output to allChannels
// change this array to only send metronome sounds to specific clients
~allChannels = Array.fill(~maxClients, { |n| n * 2; });

// from https://depts.washington.edu/dxscdoc/Help/Tutorials/Tutorial.html
// define a noise pulse, then send it to all channels
SynthDef("tish", { arg freq = 1200, bpm = 90;
    var osc, trg;
    trg = Decay2.ar(Impulse.ar(bpm/60,0,0.3), 0.01, 0.3);
    osc = {WhiteNoise.ar(trg)}.dup;
    Out.ar(~allChannels, osc);
}).send(s);

// from http://ccrma.stanford.edu/planetccrma/software/supercollider-en/four-beeps.html
// define a beep, then play it once to all channels, then free the synth
// i_* arguments are fixed at instantiation time and cannot be modulated
// the mul ugen input is usually more efficient than *
// doneAction 2 means free the synth node when the envelope has finished
SynthDef("beep", { arg amp=1.0, freq=440, i_dur=1.0;
    var env, sine;
    env = Line.kr(1, 0, i_dur, doneAction: 2, mul: amp);
    sine = SinOsc.ar(freq, mul: env);
    Out.ar(~allChannels, [sine, sine]);
}).send(s);

// run this after the mix has started
// start the metronome, and play a beep when it starts
// the value of the global variable ~rate is passed as the bpm argument to "tish"
~runAfter = {
    Synth("beep");
    Synth("tish", [\bpm, ~rate]);
};

// Uncomment either the "AutoPanMix" line or the "SimpleMix" line based on which you want to use

AutoPanMix(~maxClients).panSlots_(3).selfVolume_(1).masterVolume_(1.0).connect.start.after(~runAfter);
// SimpleMix(~maxClients).masterVolume_(1.0).connect.start.after(~runAfter);