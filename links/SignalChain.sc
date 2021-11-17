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
 * SignalChain: A sequence of Links used to process a stream of audio
 */

SignalChain : Class {
    var <>maxClients;
    var <>links;

    // creates a new signal chain object
    *new { | maxClients = 16 |
        ^super.new().maxClients_(maxClients).links_(Array.new);
    }

    // clears the signal chain
    clear {
        links = Array.new;
        ^this;
    }

    // appends a new link to the signal chain, add maxClients salt
    append { |l|
        links = links ++ l.maxClients_(maxClients);
        ^this;
    }

    // processes audio using the sequence of Links
    ar { | input, id = "" |
        var signal = input;
        
        links.size.do({ |n|
            signal = links[n].ar(signal, id);
        });
        
        ^signal;
    }

    // runs before a synth using the signal chain is started
    before { | server |
        links.size.do({ |n|
            links[n].before(server);
        });
    }

    // runs after a synth using the signal chain has started
    after { | server, synth |
        links.size.do({ |n|
            links[n].after(server, synth);
        });
    }

    // returns a unique name for this signal chain
    getName {
        var name = "";
        
        if (links.size == 0, { ^name; }, { name = "-"; });

        links.size.do({ |n|
            name = name ++ links[n].getName();
            if (n+1 < links.size, { name = name ++ "_"; });
        });

        ^name;
    }

    // returns a list of synth arguments used by this signal chain
    getArgs {
        var args = Array.new;
        
        links.size.do({ |n|
            args = args ++ links[n].getArgs();
        });

        ^args;
    }
}