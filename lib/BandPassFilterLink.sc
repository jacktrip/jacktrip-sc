BandPassFilterLink : Link {
    var<> low;
    var<> high;

	*new { | low=20, high=20000 |
		^super.new().low_(low).high_(high);
	}

    transform { |input|
        var signal = input;
        signal = LPF.ar(signal, high);
        signal = HPF.ar(signal, low);
        ^signal
    }
}