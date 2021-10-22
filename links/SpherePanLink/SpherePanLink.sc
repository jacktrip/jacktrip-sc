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
 * SpherePanLink: pans a signal in a sphere surrounding the listener
 *
 * \panSlots: if > 1, pan values will be initialized by automatically spreading
 * the audio from each client across this number of positions in the soundstage
 */

SpherePanLink : Link {
    var<> panSlots;

    *new { | panSlots = 1 |
        ^super.new().panSlots_(panSlots);
    }

    ar { |input|
        var signal = input;
        var panValues = PanningLink.autoPan(maxClients, panSlots);
        var elevationValues = SpherePanLink.autoElevation(maxClients);

        signal = SquashToMonoLink(true, false).ar(signal);
        signal = PanB.ar(signal, \pan.kr(panValues) * pi, \spherepan_elevation.kr(elevationValues));
        signal = FoaDecode.ar(signal, ~atkDecoder);
        ^signal;
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        var panValues = PanningLink.autoPan(maxClients, panSlots);
        var elevationValues = SpherePanLink.autoElevation(maxClients);
        ^[\pan, panValues, \spherepan_elevation, elevationValues];
    }

    // returns an array of initial elevation values for each client, from -0.5pi to 0.5pi
    *autoElevation { | maxClients |
        ^Array.fill(maxClients, { arg i;
            switch(i % 3, 0, {0}, 1, {0.25pi}, 2, {-0.25pi});
        });
    }
}