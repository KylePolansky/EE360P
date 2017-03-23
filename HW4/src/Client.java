import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
	public static boolean debug = true;

	public static void main(String[] args) throws URISyntaxException, IOException {
		Scanner sc = new Scanner(System.in);
		int numServer = sc.nextInt();

		ArrayList<URI> serverList = new ArrayList<>();
		for (int i = 0; i < numServer; i++) {
			serverList.add(new URI("my://" + sc.nextLine()));
		}

		ClientNetworking cn = new ClientNetworking(serverList);
		cn.Connect();

		while (sc.hasNextLine()) {
			String cmd = sc.nextLine();
			String[] tokens = cmd.split(" ");

			if (tokens[0].equals("purchase")) {
				cn.Send(cmd);
			} else if (tokens[0].equals("cancel")) {
				cn.Send(cmd);
			} else if (tokens[0].equals("search")) {
				cn.Send(cmd);
			} else if (tokens[0].equals("list")) {
				cn.Send(cmd);
			} else {
				System.out.println("ERROR: No such command");
			}
		}
	}

	static class ClientNetworking {
		static ArrayList<URI> serverList;
		static int serverToUse;
		static Socket clientSocket;
		static DataOutputStream outStream;
		static int responseReceived; // 0 = no response, 1 = need to resend, 2 = got response.

		public ClientNetworking(ArrayList<URI> servers) {
			serverList = servers;
			serverToUse = 0;
			responseReceived = 0;
		}

		public static void Connect() throws IOException {
			new Thread(new TCPListener()).start();
			SendKeepAlive();
		}

		public static synchronized void Send(String cmd) throws IOException {
			if (debug) System.out.println("DEBUG: Sending TCP Command: " + cmd);

			//Wait for socket to connect
			while (clientSocket == null || !clientSocket.isConnected());

			responseReceived = 0;
			outStream.writeBytes(cmd + '\n');
			while (responseReceived != 2) {
				if (responseReceived == 1) {
					outStream.writeBytes(cmd + '\n');
				}
			}
		}

		public static synchronized void SendKeepAlive() throws IOException {
			Send("keepalive");
		}

		static class TCPListener implements Runnable {
			private static BufferedReader reader;

			@Override
			public void run() {
				while (true) {
					if (clientSocket == null || !clientSocket.isConnected())
						Connect();

					while (clientSocket != null && clientSocket.isConnected()) {
						String line;
						try {
							while ((line = reader.readLine()) != null) {
								line = line.trim();

								if (line.equals("ack")) {
									SendKeepAlive();
								}
								else {
									System.out.println(line.trim());
								}
							}
						} catch (IOException e) {
							if (debug)
								System.out.println("DEBUG: Error in TCPListener Receive, trying new server: " + e.getStackTrace());
							clientSocket = null;
							responseReceived = 1;
						}
					}
				}
			}

			private boolean Connect() {
				while (clientSocket == null || !clientSocket.isConnected()) {
					try {
						int oldServer = serverToUse;
						serverToUse = serverToUse++ % serverList.size();
						clientSocket = new Socket();
						clientSocket.connect(new InetSocketAddress(serverList.get(oldServer).getHost(), serverList.get(oldServer).getPort()), 100);
						clientSocket.setSoTimeout(100);
						reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						outStream = new DataOutputStream(clientSocket.getOutputStream());
						return true;

					} catch (IOException e) {
						if (debug) System.out.println("DEBUG: Error in TCPListener.Connect: " + e.getStackTrace());
					}
				}

				if (debug) System.out.println("DEBUG: Error in TCPListener.Connect: (OUT OF ADDRESSES)");
				return false;
			}
		}
	}
}
