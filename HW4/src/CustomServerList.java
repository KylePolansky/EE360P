import java.util.ArrayList;

class CustomServerList {
	private ArrayList<CustomServer> customServerList;
	public int requestResponses;
	public boolean requestCanEnter;

	public CustomServerList(ArrayList<CustomServer> customServerList) {
		this.customServerList = customServerList;
	}

	public synchronized ArrayList<CustomServer> getCustomServerList() {
		return customServerList;
	}

	public synchronized void SetServerCrashed(int serverID) {
		getCustomServerList().stream().filter(s -> s.getID() == serverID).findFirst().get().setCrashed(true);
	}

	public synchronized boolean SendAllServersRequest() {
		requestResponses = 0;
		requestCanEnter = true;
		int requestsNeeded = (int) getCustomServerList().stream().filter(s -> !s.isCrashed()).count() - 1;
		String finalCommand = Server.myID + " " + Server.Mutex.getTimeStamp() + " " + "REQUEST";

		//Get response for valid threads
		getCustomServerList().stream()
				.filter(s -> !s.isCrashed())
				.forEach(s -> new Thread(new ServerSend(s.getID(), finalCommand, ServerSendType.REQUEST)).start());

		while (requestsNeeded < requestResponses);
		return requestCanEnter;
	}

	public synchronized void SendAllServersRelease(MutexRequest request) {
		String finalCommand = Server.myID + " " + request.TimeStamp + " " + "RELEASE " + request.command;

		//Get response for valid threads
		getCustomServerList().stream()
				.filter(s -> !s.isCrashed())
				.forEach(s -> new Thread(new ServerSend(s.getID(), finalCommand, ServerSendType.REQUEST)).start());
	}
}

enum ServerSendType {
	REQUEST, RELEASE
}

