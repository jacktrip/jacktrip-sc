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
 * ConvReverbLink: convolution reverb using supercollider's PartConv
 * See http://doc.sccode.org/Classes/PartConv.html
 */

ConvReverbLink : Link {
    var<> irPath;
    var<> fftsize;
    var<> mix;
    var<> low;
    var<> high;
    var irspectrum;

    *new { | irPath, mix = 0.33, low = 300, high = 6000, fftsize = 2048 |
        ^super.new().irPath_(irPath).mix_(mix).low_(low).high_(high).fftsize_(fftsize);
    }

    ar { | input |
        var signal = input;
        signal = Array.fill(signal.size, { | channelNum |
            if (signal[channelNum].isArray, {
                Array.fill(signal[channelNum].size, { | n |
                    this.process(signal[channelNum][n], n);
                });
            }, {
                this.process(signal[channelNum], channelNum);
            });
        });
        ^signal;
    }

    // process a single channel using the convolution
    process { | signal, channelNum |
        var wetMul = \convreverb_mix.kr(mix);
        var dryMul = wetMul - 1;
        var drySignal = MulAdd(signal, dryMul);
        var wetSignal = signal;
        
        wetSignal = LPF.ar(wetSignal, \convreverb_high.kr(high));
        wetSignal = HPF.ar(wetSignal, \convreverb_low.kr(low));
        wetSignal = PartConv.ar(wetSignal, fftsize, irspectrum[channelNum.mod(irspectrum.size)].bufnum, mul: wetMul);
        
        ^Mix([drySignal, wetSignal]);
    }

    // prepares impulse response spectrums used by PartConv
    before { | server |
        var numChannels, numFrames, tmpBuffer;

        tmpBuffer = Buffer.read(server, irPath, action: {
            numChannels = tmpBuffer.numChannels;
            numFrames = tmpBuffer.numFrames;
        });
        
        server.sync;
        tmpBuffer.free;

        irspectrum = Array.fill(numChannels, { | channelNum |
            var irBuf, bufsize, channelBuf;
            var gotChannel = Condition.new;
            
            channelBuf = Buffer.readChannel(server, irPath, channels: [channelNum], action: {
                bufsize = PartConv.calcBufSize(fftsize, channelBuf);
                irBuf = Buffer.alloc(server, bufsize, 1);
                irBuf.preparePartConv(channelBuf, fftsize);
            });
        
            server.sync;
            channelBuf.free;

            irBuf;
        });

        server.sync;
    }

    // returns a unique name for this Link
    getName {
        var filename = PathName.new(irPath).fileNameWithoutExtension;
        ^("ConvReverbLink-" ++ fftsize ++ "-" ++ filename);
    }

    // returns a list of synth arguments used by this Link
    getArgs {
        ^[ \convreverb_mix, mix,
           \convreverb_low, low,
           \convreverb_high, high
        ];
    }
}