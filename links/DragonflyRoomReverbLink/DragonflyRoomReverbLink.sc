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
 * DragonflyRoomReverbLink: wraps the Dragonfly Room Reverb plugin
 * See https://michaelwillis.github.io/dragonfly-reverb/
 */

DragonflyRoomReverbLink : Link {
    var<> fx;
    var<> dry;      // dry level 0-1 (percent); default=0.8
    var<> early;    // early level 0-1 (percent); default=0.1
    var<> late;     // late level 0-1 (percent); default=0.2
    var<> size;     // size 0-1; default=0.167
    var<> diffuse;  // diffuse 0-1 (percent); default=0.7
    var<> decay;    // decay 0-1; default=0.03

    *new { | dry = 0.8, early = 0.1, late = 0.2, size = 0.167, diffuse = 0.7, decay = 0.03 |
        ^super.new().dry_(dry).early_(early).late_(late).size_(size).diffuse_(diffuse).decay_(decay);
    }

    ar { | input, id = "" |
        var signal = input;
        var params = [
            0, \dragonflyroom_dry.kr(dry),
            1, \dragonflyroom_early.kr(early),
            3, \dragonflyroom_late.kr(late),
            4, \dragonflyroom_size.kr(size),
            7, \dragonflyroom_decay.kr(decay),
            8, \dragonflyroom_diffuse.kr(diffuse)
            ];
        signal = VSTPlugin.ar(signal, 2, id: "dragonflyroom"++id, params: params);
        ^signal;
    }

    before { | server |
        VSTPlugin.search(server);
    }

    after { | server, synth |
        var pluginName = "Dragonfly Room Reverb";
        fx = VSTPluginController.collect(synth);
        fx.do({ |item|
            item.open(pluginName, editor: false);
        });
    }

    getArgs {
        ^[
           \dragonflyroom_dry, dry, \dragonflyroom_early, early,
           \dragonflyroom_late, late, \dragonflyroom_size, size,
           \dragonflyroom_diffuse, diffuse, \dragonflyroom_decay, decay
        ];
    }
}
