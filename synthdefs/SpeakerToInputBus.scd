/*
 * Copyright 2023 JackTrip Labs, Inc.
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
 * SpeakerToInputBus is used to apply leveling to the speaker audio stream
 * and deliver it to the input audio bus
 *
 * \mix : array of amplitude level multipliers (default 1.0) for each client
 * \mul : master amplitude level multiplier (default 1.0)
 */

~synthDef = { | maxClients, preChain, postChain, inputChannelsPerClient = 2, outputChannelsPerClient = 2, withJamulus = false, broadcast = false |
    var defaultMix = 1 ! maxClients;
    var levels = \mix.kr(defaultMix) * \mul.kr(1.0);
    var signal = JackTripInput(1, inputChannelsPerClient, true, (maxClients - 1) * inputChannelsPerClient).getSignal();
    if (broadcast.not, {
        var metronome = MetronomeLink.new.vol_(0);
        var tuningNote = TuningNoteLink.new.vol_(0);
        signal = metronome.ar(signal);
        signal = tuningNote.ar(signal);
    });
    signal = MulAdd(signal[0], levels[maxClients - 1], 0);
    Out.ar(~inputBuses[maxClients - 1].index, signal);
}
