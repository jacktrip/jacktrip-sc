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

// allocate a stereo audio bus the handle input from each client
// the first maxClients * outputChannelsPerClient channels are reserved for audio outputs
// the subsequent maxClients * inputChannelsPerClient channels are reserved for audio inputs
// audio is read from the audio input channels and is sent back to each client through the
// audio output channels
// private buses are reserved for internally routing audio signals

~makeInputBusses = { | server, maxClients, inputChannelsPerClient, outputChannelsPerClient |
    ~firstPrivateBus = (maxClients * (inputChannelsPerClient + outputChannelsPerClient));
    ~inputBuses = Array.fill(maxClients, { |n|
        var i = ~firstPrivateBus + (n * inputChannelsPerClient);
        Bus.new('audio', i, inputChannelsPerClient, server);
    });
}
