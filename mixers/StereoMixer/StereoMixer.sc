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
 * StereoMixer: flat stereo mix that supports personal mixes up to 100 clients
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 * \hpf: sets the default high-pass filter frequency used for all clients
 * \lpf: sets the default low-pass filter frequency used for all clients
 */

StereoMixer : PersonalMixer {
    
    // the following parameters are instance variables
    // the '<' is shorthand for a getter method and '>' is shorthand for a setter method
    var <>hpf, <>lpf;

    // create a new instance
    *new { | maxClients = 16, hpf = 20, lpf = 20000 |
        ^super.new(maxClients).hpf_(hpf).lpf_(lpf);
    }

    // starts up all the audio on the server
    start {

        // prepare pre processing signal chain
        this.preChain.clear().maxClients_(maxClients);
        this.preChain.append(BandPassFilterLink().low_(hpf).high_(lpf));

        // start InputBusMixer and PersonalMixer base classes
        super.start();
    }
}