/*
 * BaseMix: base class for other JackTrip Virtual Studio mixers
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 */
BaseMix : Object {
	var <>maxClients;
	var <>masterVolume = 1.0;		// master volume level multiplier
	var <>serverIp = "127.0.0.1";	// IP address or hostname of remote audio server
	var <>serverPort = 57110;		// port number of remote audio server
	var <>serverReady, <>server;
	var <>mixStarted;
	classvar inputChannelsPerClient = 2;
	classvar outputChannelsPerClient = 2;

	// create a new instance
	*new { | maxClients = 16 |
		^super.newCopyArgs(maxClients).serverReady_(Condition.new).mixStarted_(Condition.new);
	}

	// connect to a remote server
	connect {
		var waitForServer = Routine {
			var retries = 10;
			server.notify;
			server.initTree;

			{retries > 0}.while({
				server.serverRunning.postln;
				Server.allRunningServers.postln;
				if (Server.allRunningServers != [], {
					"Server started".postln;
					retries = 0;
				}, {
					"Waiting for server".postln;
					retries = retries - 1;
					1.sleep;
				});
			});

			serverReady.test = true;
			serverReady.signal;
		};

		("Connecting to server"+serverIp++":"++serverPort).postln;
		server = Server.remote(\remote, NetAddr(serverIp, serverPort));
		server.doWhenBooted({waitForServer.value});
	}

	// wait for mix to start
	wait {
		mixStarted.wait;
	}

	// run a routine after mix has started
	after { | r |
		Routine {
			this.wait;
			r.value;
		}.run;
	}
}
