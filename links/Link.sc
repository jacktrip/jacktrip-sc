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
 * Link: A stage in a signal flow chain.
 *
 * A Link is an abstract representation of an element or phase in a
 * more complicated signal flow processing chain. A Link is initialized
 * with certain settings passed into the constructor, then the ar
 * function is called. The ar function accepts an audio signal and
 * outputs another audio signal. This allows links to be chained together
 * to create signal flow chains.
 *
 * var signal = SoundIn(inputChannel);
 * signal = Link1().ar(signal);
 * signal = Link2().ar(signal);
 * signal = Link3().ar(signal);
 * Out(outputChannel, signal)
 * 
 * This can be thought of as:
 *
 * Input Signal => Link 1 => Link 2 => Link 3 => Output
 * 
 * The transformations that links can do to signals may vary. A Link
 * may make assumptions about the input signals, including whether or not
 * they are arrays comprised of individual audio channels. Links may also
 * modify the signals by changing their dimensionality. For example, the
 * AggregateLink performs a sum operation to effectively flatten a 2D array.
 * Additionally, transformations may invoke multichannel expansion, depending
 * on the parameters and input signals.
 */

Link : Class {
    var<> maxClients;
    var<> withJamulus;

    *new { | maxClients = 16, withJamulus = true |
        ^super.new().maxClients_(maxClients).withJamulus_(withJamulus);
    }

    // Override this function!
    ar { | input, id = "" |
        ^input
    }

    // runs before a synth using the signal chain is started
    before { | server | }

    // runs after a synth using the signal chain has started
    after { | server, synth | }

    // returns a unique name for this Link
    getName { ^this.class.asString; }

    // returns a list of synth arguments used by this Link
    getArgs { ^nil }
}