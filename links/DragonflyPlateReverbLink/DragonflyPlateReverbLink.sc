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
 * DragonflyPlateReverbLink: wraps the Dragonfly Plate Reverb plugin
 * See https://michaelwillis.github.io/dragonfly-reverb/
 */

DragonflyPlateReverbLink : Link {
    var<> fx;
    var<> dry;      // dry level 0-1 (percent); default=0.8
    var<> wet;      // wet level 0-1 (percent); default=0.2
    var<> decay;     // late level 0-1 (percent); default=0.03

    *new { | dry = 0.8, wet = 0.2, decay = 0.03 |
        ^super.new().dry_(dry).wet_(wet).decay_(decay);
    }

    ar { | input, id = "" |
        var signal = input;
        var params = [
            0, \dragonflyplate_dry.kr(dry),
            1, \dragonflyplate_wet.kr(wet),
            5, \dragonflyplate_decay.kr(decay)
            ];
        signal = VSTPlugin.ar(signal, 2, id: "dragonflyplate"++id, params: params);
        ^signal;
    }

    before { | server |
        VSTPlugin.search(server);
    }

    after { | server, synth |
        var pluginName = "Dragonfly Plate Reverb";
        fx = VSTPluginController.collect(synth);
        fx.do({ |item|
            item.open(pluginName, editor: false);
        });
    }

    getArgs {
        ^[
           \dragonflyplate_dry, dry,
           \dragonflyplate_wet, wet,
           \dragonflyplate_decay, decay
        ];
    }
}
