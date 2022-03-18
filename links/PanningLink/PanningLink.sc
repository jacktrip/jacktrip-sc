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
 *
 * \panSlots: if > 1, pan values will be initialized by automatically spreading
 * the audio from each client across this number of positions in the soundstage
 */

PanningLink : Link {
    var<> panSlots;
    var<> left;
    var<> right;

    *new { | panSlots = 1, left = -0.5, right = 0.5 |
        ^super.new().panSlots_(panSlots).left_(left).right_(right);
    }

    ar { | input, id = "" |
        var signal = input;
        var panValues = PanningLink.autoPan(maxClients, panSlots, left, right);
        
        signal = SquashToMonoLink(true, false).ar(signal);
        signal = Pan2.ar(signal, \pan.kr(panValues));
        ^signal;
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        var panValues = PanningLink.autoPan(maxClients, panSlots, left, right);
        if(withJamulus, {
            panValues = panValues.insert(0, 0);
            panValues.removeAt(maxClients);
        });
        ^[\pan, panValues];
    }

    // returns an array of initial pan values for each client, from left to right (-1 to 1)
    *autoPan { | maxClients, slots, left, right |
        var panValues = Array.new;

        // automatically pan clients across stereo field
        if (slots > maxClients, { slots = maxClients; });
        if (slots < 2, {
            panValues = [ (left + right) / 2 ];
            slots = 1;
        }, {
            var midpoint = (slots/2).trunc.asInteger;

            // LinLin maps a range of input values linearly to a range of
            // output values
            var unsorted = Array.fill(slots, { arg i;
                LinLin.kr(i % slots, 0, slots - 1, left, right);
            });

            // start with middle if odd
            if ((slots/2) != (slots/2).trunc, {
                panValues = [ unsorted.removeAt(midpoint); ];
            });

            // expand out from the midpoint
            midpoint.do{ arg i;
                panValues = panValues ++ [ unsorted[midpoint - i - 1], unsorted[midpoint + i] ];
            };
        });

        ("panning" + maxClients + "clients across" + slots + "slots:" + panValues).postln;

        ^Array.fill(maxClients, { arg i;
            panValues[i % slots];
        });
    }
}