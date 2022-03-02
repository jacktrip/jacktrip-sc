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
 * JackTripMixerGUI: a simple GUI for remotely controlling mixers
 */

JackTripMixerGUI : BaseMixer {
    
    // create a new instance
    // * \maxClients: maximum number of clients that may connect to the audio server
    *new { | maxClients = 16 |
        ^super.new(maxClients);
    }

    // display a graphical user interface for mixing controls
    // * \maxSlidersPerRow: maximum number of sliders displayed per row
    // * \maxMultiplier: maximum amplitude multiplier to use for sliders
    open { | maxSlidersPerRow = 20, maxMultiplier = 5 |
        var g;
        var mix = 1 ! maxClients;
        var pan = 0 ! maxClients;
        var rows = 1;
        var cols = maxClients + 1;
        var window;
        var master;
        var sliders = Array.newClear(maxClients);
        var panKnobs = Array.newClear(maxClients);
        var serverInput;
        var connectButton;
        var x = 0;
        var y = 0;

        // run this when connect button is clicked
        var connectToServer = Routine {
            // update serverIp and connect to remote server
            serverIp = serverInput.string.stripWhiteSpace;
            this.connect;
            serverReady.wait;

            defer {
                // initialize levels to 1.0
                g = ParGroup.basicNew(server, 100);
                g.set(\mul, 1);
                g.set(\mix, mix);
                //g.set(\pan, pan); // don't reset because this breaks autopan
                master.value = 1.0 / maxMultiplier;
                maxClients.do({ arg n;
                    sliders[n].value = 1.0 / maxMultiplier;
                    panKnobs[n].value = 0;
                });
            };
        };

        // prepare slider grid
        if (maxClients >= maxSlidersPerRow, {
            rows = (maxClients + 1 / maxSlidersPerRow).roundUp;
            cols = maxSlidersPerRow;
        });

        // create a new window
        window = Window.new("JackTrip Mixer", Rect(50,50, ((50*cols)+30).max(480),(300*rows)+60));

        // add master slider
        master = Slider.new(window, Rect(20,80,40,200));
        master.background = "black";
        master.action_( { arg me;
            var mul = me.value * maxMultiplier;
            ("master vol ="+mul).postln;
            g.set(\mul, mul);
        });
        StaticText(window, Rect(20, 280, 40, 20)).string_("Master");

        // add controls for each client
        maxClients.do({ arg n;
            if ((x+1) < cols, { x = x + 1; }, { x = 0; y = y + 1; });

            sliders[n] = Slider.new(window, Rect(20+(x*50), 80+(300*y), 40, 200)).action_( { arg me;
                var mul = me.value * maxMultiplier;
                ("ch"+n+"vol ="+mul).postln;
                // used mix for JackTripPannedIn (master)
                // this ensures it works regardless of whether personal mixes are being used, or not
                mix[n] = mul;
                g.set(\mix, mix);
            });

            StaticText(window, Rect(30+(x*50), 280+(300*y), 40, 20)).string_(n);

            panKnobs[n] = Knob.new(window, Rect(20+(x*50), 305+(300*y), 40, 40)).action_( { arg me;
                var p = LinLin.kr(me.value, 0, 1, -1, 1);
                ("ch"+n+"pan ="+p).postln;
                pan[n] = p;
                g.set(\pan, pan);
            });
        });

        // add input box for server and connect button
        serverInput = TextField(window, Rect(20,20,300,40)).value_(serverIp);
        connectButton = Button(window, Rect(340,20,100,40));
        connectButton.states_([["Connect"]]);
        connectButton.action_(connectToServer);

        // display the window
        window.front;
    }
}
