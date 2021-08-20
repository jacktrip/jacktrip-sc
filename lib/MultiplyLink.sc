MultiplyLink : Link {
    var<> volume;

	*new { | volume=1.0 |
		^super.new().volume_(volume);
	}

    transform { |input|
        ^MulAdd(input, volume, 0)
    }
}