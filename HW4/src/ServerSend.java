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
		try {
			CustomServer server = Server.ServerList.getServerByID(serverID);

			Socket sock = new Socket();
			sock.connect(new InetSocketAddress(server.getUri().getHost() , server.getUri().getPort()),Client.timeout);
			sock.setSoTimeout(Client.timeout);

			BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			DataOutputStream output = new DataOutputStream(sock.getOutputStream());

			String outputText = sendCommand + "\n";
			output.writeBytes(outputText);
			output.flush();
			if (Server.debug) System.out.println("DEBUG: Sending output to server: " + outputText);

			switch (commandType) {
				case REQUEST:
					String response = input.readLine();
					if (Server.debug) System.out.println("DEBUG: Response from server: " + outputText);
					int responseServerID = Integer.parseInt(response.split(" ")[0]);
					int responseTimeStamp = Integer.parseInt(response.split(" ")[1]);
					if (responseTimeStamp < Server.Mutex.myTimestamp.get()) {
						Server.ServerList.requestCanEnter = false;
					}
					else if (responseTimeStamp == Server.Mutex.myTimestamp.get()) {
						Server.ServerList.requestCanEnter = responseServerID > Server.myID;
					}
					Server.ServerList.requestResponses.incrementAndGet();
					break;
				case RELEASE:
					//No response needed
					break;
			}
			//TODO: handle response

			sock.close();
			return;

		} catch (IOException e) {
			if (Server.debug) System.out.println("DEBUG: Cannot Connect to server: " + serverID);
			e.printStackTrace();
			Server.ServerList.SetServerCrashed(serverID);

			switch (commandType) {
				case REQUEST:
					Server.ServerList.requestResponses.incrementAndGet();
					break;
				case RELEASE:
					//No response needed
					break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
