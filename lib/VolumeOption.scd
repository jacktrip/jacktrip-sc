VolumeOption : Option {
    var<> volume;

	*new { | volume=1.0 |
		^super.new().volume_(volume);
	}

    transform { |input|
        ^input.madd(volume, 0);
    }
}