/* 
 * Copyright 2020-2021 JackTrip Labs, Inc.
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
 * MonoPanWithPersonalMixer: automatically pans clients across a stereo sound field,
 *                           and supports personal mixes up to 100 clients
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \panSlots: number of panning slots to use across a stereo sound field
 * \hpf: sets the default high-pass filter frequency used for all clients
 * \lpf: sets the default low-pass filter frequency used for all clients
 */

MonoPanWithPersonalMixer : PersonalMixer {
    
    // the following parameters are instance variables
    // the '<' is shorthand for a getter method and '>' is shorthand for a setter method
    var <>panSlots = 1;
    var <>hpf = 20;
    var <>lpf = 20000;

    var <>gate_thresh = -50;
    var <>gate_attack = 0.01;
    var <>gate_release = 0.01;
    var <>gate_range = 10;

    var <>compressor_thresh = -10;
    var <>compressor_attack = 0.01;
    var <>compressor_release = 0.02;
    var <>compressor_ratio = 0.5;

    var <>limiter_thresh = -2;
    var <>limiter_attack = 0.002;
    var <>limiter_release = 0.01;
    var <>limiter_ratio = 0.1;

    var<> freeverb2_mix = 0.25;
    var<> freeverb2_room = 0.15;
    var<> freeverb2_damp = 0.5;

    // create a new instance
    *new { | maxClients = 16 |
        ^super.new(maxClients);
    }

    // starts up all the audio on the server
    start {

        // prepare pre processing signal chain
        // squash to mono and pan clients
        this.preChain.clear().maxClients_(maxClients);
        this.preChain.append(BandPassFilterLink().low_(hpf).high_(lpf));
        this.preChain.append(GateLink().thresh_(gate_thresh).attack_(gate_attack).release_(gate_release).range_(gate_range));
        this.preChain.append(CompressorLink().thresh_(compressor_thresh).attack_(compressor_attack).release_(compressor_release).ratio_(compressor_ratio));
        this.preChain.append(LimiterLink().thresh_(limiter_thresh).attack_(limiter_attack).release_(limiter_release).ratio_(limiter_ratio));
        this.preChain.append(PanningLink().maxClients_(maxClients).panSlots_(panSlots));

        this.postChain.clear().maxClients_(maxClients);
        this.postChain.append(FreeVerb2Link().mix_(freeverb2_mix).room_(freeverb2_room).damp_(freeverb2_damp));

        // start InputBusMixer and PersonalMixer base classes
        super.start();
    }
}
