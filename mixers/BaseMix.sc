/*
 * BaseMix: base class for other JackTrip Virtual Studio mixers
 *
 * \maxClients: maximum number of clients that may connect to the audio server
 */
BaseMix : Object {
	var <>maxClients;
	var <>withJamulus = false;		// create mixes adapted Jamulus being connected on channels 1 & 2
	var <>masterVolume = 1.0;		// master volume level multiplier
	var <>serverIp = "127.0.0.1";	// IP address or hostname of remote audio server
	var <>serverPort = 57110;		// port number of remote audio server (default SC port)
	var <>serverReady, <>server;    // state of the server and the server object
	var <>mixStarted;				// Condition object used to pause execution until the server is ready
	var <>defaultMix;				// default master mix is just an array of ones (do nothing)
	classvar inputChannelsPerClient = 2;	// for stereo audio inputs
	classvar outputChannelsPerClient = 2;	// for stereo audio outputs

	// create a new instance
	*new { | maxClients = 16 |
		^super.newCopyArgs(maxClients).serverReady_(Condition.new).mixStarted_(Condition.new).defaultMix_(1 ! maxClients);
	}

	// connect to a remote server
	connect {
		var waitForServer = Routine {
			var retries = 10;
			server.notify;
			server.initTree;

			// Make 10 attempts to connect to the server
			{retries > 0}.while({
				//server.serverRunning.postln;
				//Server.allRunningServers.postln;
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

		// Create a new server object using the IP address and Port
		// server can be used as one might use the 's' global variable locally
		("Connecting to server"+serverIp++":"++serverPort).postln;
		server = Server.remote(\remote, NetAddr(serverIp, serverPort));
		server.doWhenBooted({waitForServer.value});
	}

	// wait for mix to start
	// mixStarted is the Condition object and pauses execution until
	// the value is set to true and mixStarted is signalled
	wait {
		mixStarted.wait;
	}

	// run a routine after mix has started
	// mixStarted must have value of true and must be signalled
	// in order for r to run
	after { | r |
		Routine {
			this.wait;
			r.value;
		}.run;
	}
}
