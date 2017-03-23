import java.net.URI;

public class CustomServer {
	private URI uri;
	private boolean isCrashed;
	private int ID;

	public CustomServer(int ID, URI uri) {
		this.uri = uri;
		isCrashed = false;
		this.ID = ID;
	}

	public synchronized URI getUri() {
		return uri;
	}

	public synchronized boolean isCrashed() {
		return isCrashed;
	}

	public synchronized void setCrashed(boolean crashed) {
		isCrashed = crashed;
	}

	public int getID() {
		return ID;
	}
}