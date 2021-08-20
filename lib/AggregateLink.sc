AggregateLink : Link {
    *new {
		^super.new();
	}

    transform { |input|
        ^Mix(input);
    }
}