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
 * TalReverbLink: wraps the TAL Reverb 4 vst3 plugin
 * See https://tal-software.com/products/tal-reverb-4
 */

TalReverbLink : Link {
    var<> fx;
    var<> wet;      // 0-1 (percent); default=0.2
    var<> dry;      // 0-1 (percent); default=1.0
    var<> delay;    // 0-1 (seconds); default=0
    var<> size;     // 0-1 (percent); default=0.85
    var<> high;     // high-cut filter 0-1; default=0.8 (8192 hz)
    var<> low;      // low-cut filter 0-1; default=0
    var<> modrate;  // modulation rate 0-100 (percent); default=0.5
    var<> moddepth; // modulation depth 0-100 (percent); default=0.2
    var<> diffuse;  // 0-1 (percent); default=0.9

    *new { | wet = 0.2, dry = 1.0, delay = 0, size = 0.85, high = 0.8, low = 0, modrate = 0.5, moddepth = 0.2, diffuse = 0.9 |
        ^super.new().wet_(wet).dry_(dry).delay_(delay).size_(size).high_(high).low_(low).modrate_(modrate).moddepth_(moddepth).diffuse_(diffuse);
    }

    ar { | input, id = "" |
        var signal = input;
        var params = [0, 1,
            1, \talreverb_wet.kr(wet),
            2, \talreverb_dry.kr(dry),
            3, \talreverb_delay.kr(delay),
            4, \talreverb_size.kr(size),
            5, \talreverb_high.kr(high),
            6, \talreverb_low.kr(low),
            7, \talreverb_modrate.kr(modrate),
            8, \talreverb_moddepth.kr(moddepth),
            9, \talreverb_diffuse.kr(diffuse)];
        signal = VSTPlugin.ar(signal, 2, id: "talreverb"++id, params: params);
        ^signal;
    }

    before { | server |
        VSTPlugin.search(server);
    }

    after { | server, synth |
        var pluginName = "TAL Reverb 4 Plugin.vst3";
        fx = VSTPluginController.collect(synth);
        fx.do({ |item|
            item.open(pluginName, editor: false);
        });
    }

    getArgs {
        ^[ \talreverb_wet, wet, \talreverb_dry, dry,
           \talreverb_delay, delay, \talreverb_size, size,
           \talreverb_high, high, \talreverb_low, low,
           \talreverb_modrate, modrate, \talreverb_moddepth, moddepth,
           \talreverb_diffuse, diffuse
        ];
    }
}