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
 * BandPassFilterLink: applies a low-pass
 * and high-pass filter to a signal.
 */

BandPassFilterLink : Link {
    var<> low;
    var<> high;

	*new { | low=20, high=20000 |
		^super.new().low_(low).high_(high);
	}

    ar { |input|
        var signal = input;
        signal = LPF.ar(signal, \high.kr(high));
        signal = HPF.ar(signal, \low.kr(low));
        ^signal
    }

	// returns a list of synth arguments used by this Link
	getArgs {
        ^[\low, low, \high, high];
    }
}