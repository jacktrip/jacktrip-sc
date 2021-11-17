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
 * DragonflyHallReverbLink: wraps the Dragonfly Hall Reverb plugin
 * See https://michaelwillis.github.io/dragonfly-reverb/
 */

DragonflyHallReverbLink : Link {
    var<> fx;
    var<> dry;      // dry level 0-1 (percent); default=0.8
    var<> early;    // early level 0-1 (percent); default=0.1
    var<> late;     // late level 0-1 (percent); default=0.2
    var<> size;     // size 0-1; default=0.28 (24 m)
    var<> diffuse;  // diffuse 0-1 (percent); default=0.9
    var<> decay;    // decay 0-1; default=0.12

    *new { | dry = 0.8, early = 0.1, late = 0.2, size = 0.28, diffuse = 0.9, decay = 0.12 |
        ^super.new().dry_(dry).early_(early).late_(late).size_(size).diffuse_(diffuse).decay_(decay);
    }

    ar { | input, id = "" |
        var signal = input;
        var params = [
            0, \dragonflyhall_dry.kr(dry),
            1, \dragonflyhall_early.kr(early),
            2, \dragonflyhall_late.kr(late),
            3, \dragonflyhall_size.kr(size),
            6, \dragonflyhall_diffuse.kr(diffuse),
            15, \dragonflyhall_decay.kr(decay)
            ];
        signal = VSTPlugin.ar(signal, 2, id: "dragonflyhall"++id, params: params);
        ^signal;
    }

    before { | server |
        VSTPlugin.search(server);
    }

    after { | server, synth |
        var pluginName = "Dragonfly Hall Reverb";
        fx = VSTPluginController.collect(synth);
        fx.do({ |item|
            item.open(pluginName, editor: false);
        });
    }

    getArgs {
        ^[
           \dragonflyhall_dry, dry, \dragonflyhall_early, early,
           \dragonflyhall_late, late, \dragonflyhall_size, size,
           \dragonflyhall_diffuse, diffuse, \dragonflyhall_decay, decay
        ];
    }
}
