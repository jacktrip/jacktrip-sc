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
 * PanningLink: pans a signal across a stereo field.
 *
 * If the input signal and the pan value are arrays of the same dimension, then
 * multichannel expansion will be invoked and each channel will be panned according
 * the corresponding value in the pan array.
 */

PanningLink : Link {
    var<> pan;

	*new { | pan=0 |
		^super.new().pan_(pan);
	}

    transform { |input|
        var signal;
        signal = Pan2.ar(input, pan);
        ^signal;
    }

    // returns an array of initial pan values for each client, from -1 to 1
    *autoPan { | maxClients, slots |
        var panValues;

        // automatically pan clients across stereo field
        if (slots > maxClients, { slots = maxClients; });
        if (slots < 2, {
            panValues = [0];
            slots = 1;
        }, {
            // LinLin maps a range of input values linearly to a range of
            // output values
            panValues = Array.fill(slots, { arg i;
                LinLin.kr((i % slots) + 1, 0, slots + 1, -1, 1);
            });
        });

        ("panning" + maxClients + "clients across" + slots + "slots:" + panValues).postln;

        ^Array.fill(maxClients, { arg i;
            panValues[i % slots];
        });
    }
}