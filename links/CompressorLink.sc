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
 * CompressorLink: applies downward compression to an audio signal if the
 * specified ratio is between 0 and 1.
 */

CompressorLink : Link {
    var<> threshDB;
    var<> ratio;

    *new { | threshDB = 0, ratio = 1 |
        ^super.new().threshDB_(threshDB).ratio_(ratio);
    }

    ar { |input|
        var signal = input;
        signal = Compander.ar(signal, signal, threshDB.dbamp, 1, ratio);
        ^signal
    }
}