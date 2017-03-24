import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
	public static boolean debug = true;
	public static int timeout = 10000;

	public static void main(String[] args) throws URISyntaxException, IOException {
		Scanner sc = new Scanner(System.in);
		int numServer = sc.nextInt();

		ArrayList<URI> serverList = new ArrayList<>();
		for (int i = 0; i < numServer; i++) {
			serverList.add(new URI("my://" + sc.next()));
		}

		ClientNetworking cn = new ClientNetworking(serverList);
		cn.Connect();

		while (sc.hasNextLine()) {
			String cmd = sc.nextLine();
			String[] tokens = cmd.split(" ");

			switch(tokens[0]) {
				case "purchase":
				case "cancel":
				case "search":
				case "list":
					cn.Send(cmd);
					break;
				default:
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
		}

		static synchronized void Send(String cmd) {
			new Thread(new SendThread(cmd)).start();
		}

		//TODO: This should be synchronized somehow.
		static class SendThread implements Runnable{
			private String cmd;
			public SendThread(String cmd) {
				this.cmd = cmd;
			}

			@Override
			public void run() {
				if (debug && !cmd.equals("keepalive")) System.out.println("DEBUG: Sending TCP Command: " + cmd);

				//Wait for socket to connect
				while (clientSocket == null || !clientSocket.isConnected()) ;

				responseReceived = 0;
				output();

				Thread.yield();

				while (responseReceived != 2) {
					Thread.yield();
					if (responseReceived == 1) {
						output();
						responseReceived = 0;
					}
				}
			}

			private void output() {
				try {
					synchronized (outStream) {
						outStream.writeBytes(cmd + '\n');
						outStream.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
					responseReceived = 1;
				}
			}
		}

		public static synchronized void SendKeepAlive() throws IOException {
			new Thread(new SendThread("keepalive")).start();
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
							while ((line = reader.readLine()) != null && clientSocket != null && clientSocket.isConnected()) {
								line = line.trim();
								if (debug && !line.equals("ack")) System.out.println("DEBUG: Received message: " + line);

								if (line.equals("ack")) {
									SendKeepAlive();
								}
								else {
									System.out.println(line.trim());
								}
								responseReceived = 2;
							}
						} catch (IOException e) {
							if (debug) System.out.println("DEBUG: Error in TCPListener Receive, trying new server: " + e.getMessage());
							clientSocket = null;
							responseReceived = 1;
						}
					}
				}
			}

			private synchronized void Connect() {
				while (clientSocket == null || !clientSocket.isConnected()) {
					try {
						int oldServer = serverToUse;
						serverToUse = (serverToUse + 1) % serverList.size();
						clientSocket = new Socket();
						if (debug) System.out.println("DEBUG: Trying to connect to server: " + oldServer);
						if (debug) System.out.println("DEBUG: Host: " + serverList.get(oldServer).getHost());
						if (debug) System.out.println("DEBUG: Port: " + serverList.get(oldServer).getPort());
						clientSocket.connect(new InetSocketAddress(serverList.get(oldServer).getHost(), serverList.get(oldServer).getPort()), timeout);
						if (debug) System.out.println("DEBUG: Connected to server: " + oldServer);
						clientSocket.setSoTimeout(timeout);
						if (debug) System.out.println("DEBUG: Setting input reader on server: " + oldServer);
						reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						if (debug) System.out.println("DEBUG: Setting output reader on server: " + oldServer);
						outStream = new DataOutputStream(clientSocket.getOutputStream());
						if (debug) System.out.println("DEBUG: Sending keepalive to server: " + oldServer);
						SendKeepAlive();
					} catch (IOException e) {
						if (debug) System.out.println("DEBUG: Error in TCPListener.Connect: " + e.getStackTrace());
					}
				}
			}
		}
	}
}
