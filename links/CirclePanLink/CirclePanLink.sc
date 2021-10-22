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
 * CirclePanLink: pans a signal in a circle surrounding the listener
 *
 * \panSlots: if > 1, pan values will be initialized by automatically spreading
 * the audio from each client across this number of positions in the soundstage
 */

CirclePanLink : Link {
    var<> panSlots;

    *new { | panSlots = 1 |
        ^super.new().panSlots_(panSlots);
    }

    ar { |input|
        var signal = input;
        var panValues = PanningLink.autoPan(maxClients, panSlots);
        
        signal = SquashToMonoLink(true, false).ar(signal);
        signal = PanB2.ar(signal, \pan.kr(panValues));
        signal = FoaDecode.ar(signal, ~atkDecoder);
        ^signal;
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        var panValues = PanningLink.autoPan(maxClients, panSlots);
        ^[\pan, panValues];
    }
}