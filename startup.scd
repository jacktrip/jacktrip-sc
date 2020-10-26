~serverReady = Condition.new;

~waitForServer = Routine {
	var retries = 10;
	s.notify;
	s.initTree;

	{retries > 0}.while({
		s.serverRunning.postln;
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

	~serverReady.test = true;
	~serverReady.signal;
};

"Connecting to server".postln;
Server.default = s = Server.remote(\local, NetAddr("127.0.0.1", 57110));
s.doWhenBooted({~waitForServer.value});
