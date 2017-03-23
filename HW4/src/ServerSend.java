import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServerSend implements Runnable {
	private int serverID;
	private String sendCommand;
	private ServerSendType commandType;

	public ServerSend(int serverNumber, String command, ServerSendType commandType) {
		this.serverID = serverNumber;
		this.sendCommand = command;
		this.commandType = commandType;
	}

	@Override
	public void run() {
		CustomServer server = Server.ServerList.getCustomServerList().stream().filter(s -> s.getID() == serverID).findFirst().get();

		try {
			Socket sock = new Socket();
			sock.connect(new InetSocketAddress(server.getUri().getHost() , server.getUri().getPort()),100);
			sock.setSoTimeout(100);

			BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			DataOutputStream output = new DataOutputStream(sock.getOutputStream());

			output.writeBytes(sendCommand + "\n");
			output.flush();

			String response = input.readLine();
			switch (commandType) {
				case REQUEST:
					int responseServerID = Integer.parseInt(response.split(" ")[0]);
					int responseTimeStamp = Integer.parseInt(response.split(" ")[1]);
					if (responseTimeStamp < Server.Mutex.getTimeStamp()) {
						Server.ServerList.requestCanEnter = false;
					}
					else if (responseTimeStamp == Server.Mutex.getTimeStamp()) {
						Server.ServerList.requestCanEnter = responseServerID > Server.myID;
					}
					Server.ServerList.requestResponses = Server.ServerList.requestResponses + 1;
					break;
				case RELEASE:
					//No response needed
					break;
			}
			//TODO: handle response

			sock.close();

		} catch (IOException e) {
			if (Server.debug) System.out.println("DEBUG: Cannot Connect to server " + e.getStackTrace());
			Server.ServerList.SetServerCrashed(server.getID());

			switch (commandType) {
				case REQUEST:
					Server.ServerList.requestResponses = Server.ServerList.requestResponses + 1;
					break;
				case RELEASE:
					//No response needed
					break;
			}
		}

	}
}
