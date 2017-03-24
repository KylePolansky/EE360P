import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class CustomServerList {
	private ArrayList<CustomServer> customServerList;
	public AtomicInteger requestResponses;
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
		requestResponses = new AtomicInteger(0);
		requestCanEnter = true;
		int requestsNeeded = (int) getCustomServerList().stream().filter(s -> !s.isCrashed()).count() - 1;
		String finalCommand = Server.myID + " " + Server.Mutex.getTimeStamp() + " " + "REQUEST";

		//Get response for valid threads
		getCustomServerList().stream()
				.filter(s -> !s.isCrashed() && s.getID() != Server.myID)
				.forEach(s -> new Thread(new ServerSend(s.getID(), finalCommand, ServerSendType.REQUEST)).start());

		while (requestsNeeded > requestResponses.get()) {
			Thread.yield();
		}
		return requestCanEnter;
	}

	public synchronized void SendAllServersRelease(MutexRequest request) {
		String finalCommand = Server.myID + " " + request.TimeStamp + " " + "RELEASE " + request.command;

		//Get response for valid threads
		getCustomServerList().stream()
				.filter(s -> !s.isCrashed() && s.getID() != Server.myID)
				.forEach(s -> new Thread(new ServerSend(s.getID(), finalCommand, ServerSendType.REQUEST)).start());
	}
}

enum ServerSendType {
	REQUEST, RELEASE
}

