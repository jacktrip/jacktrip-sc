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
 * DragonflyEarlyReflectionsReverbLink: wraps the Dragonfly Early Reflections Reverb plugin
 * See https://michaelwillis.github.io/dragonfly-reverb/
 */

DragonflyEarlyReflectionsReverbLink : Link {
    var<> fx;
    var<> dry;      // dry level 0-1 (percent); default=0.8
    var<> wet;      // wet level 0-1 (percent); default=0.2
    var<> size;     // size 0-1; default=0.2

    *new { | dry = 0.8, wet = 0.2, size = 0.2 |
        ^super.new().dry_(dry).wet_(wet).size_(size);
    }

    ar { | input, id = "" |
        var signal = input;
        var params = [
            0, \dragonflyearlyreflections_dry.kr(dry),
            1, \dragonflyearlyreflections_wet.kr(wet),
            3, \dragonflyearlyreflections_size.kr(size)
            ];
        signal = VSTPlugin.ar(signal, 2, id: "dragonflyearlyreflections"++id, params: params);
        ^signal;
    }

    before { | server |
        VSTPlugin.search(server);
    }

    after { | server, synth |
        var pluginName = "Dragonfly Early Reflections";
        fx = VSTPluginController.collect(synth);
        fx.do({ |item|
            item.open(pluginName, editor: false);
        });
    }

    getArgs {
        ^[
           \dragonflyearlyreflections_dry, dry,
           \dragonflyearlyreflections_wet, wet,
           \dragonflyearlyreflections_size, size
        ];
    }
}
