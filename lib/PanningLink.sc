PanningLink : Link {
    var<> pan;

	*new { | pan=0 |
		^super.new().pan_(pan);
	}

    transform { |input|
        var signal;
        signal = Pan2.ar(input, pan);
        ^signal;
    }
}