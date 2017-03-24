import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	public static final boolean debug = true;
	public static CustomServerList ServerList;
	public static int myID;
	public static ProductHandler ph;

	public static void main(String[] args) throws URISyntaxException {

		Scanner sc = new Scanner(System.in);
		myID = sc.nextInt();
		int numServer = sc.nextInt();
		String inventoryPath = sc.next();

		System.out.println("[DEBUG] my id: " + myID);
		System.out.println("[DEBUG] numServer: " + numServer);
		System.out.println("[DEBUG] inventory path: " + inventoryPath);

		ArrayList<CustomServer> slist = new ArrayList<>();
		for (int i = 0; i < numServer; i++) {
			String str = sc.next();
			slist.add( new CustomServer(i + 1, new URI("my://" + str)));
			System.out.println("address for server " + i + ": " + str);
		}

		ServerList = new CustomServerList(slist);
		int listenPort = ServerList.getCustomServerList().stream().filter(s -> s.getID() == myID).findFirst().get().getUri().getPort();
		if (debug) System.out.println("[DEBUG] starting listener on port: " + listenPort);
		new Thread(new ServerListener(listenPort)).start();

		if (debug) System.out.println("[DEBUG] parsing inventory");

		ph = new ProductHandler();
		ph.readInventory(inventoryPath);

		if (debug) System.out.println("[DEBUG] Server Running");

		while (true) {
		}
	}

	static class ServerListener implements Runnable {
		ServerSocket sock;
		int port;
		public ServerListener(int port) {
			this.port = port;
		}

		@Override
		public void run() {
			openTcpServerSocket();
			ExecutorService es = Executors.newCachedThreadPool();
			while(true){
				Socket clientSocket = null;
				try {
					clientSocket = sock.accept();
					clientSocket.setSoTimeout(Client.timeout);
					es.submit(new ServerSocketHandler(clientSocket));
				} catch (IOException e) {
					System.out.println("Error accepting new client socket") ;
				}
			}
		}

		private void openTcpServerSocket() {
			try {
				sock = new ServerSocket(port);
			} catch (IOException e) {
				throw new RuntimeException("Cannot start TCP server", e);
			}
		}
	}

	static class ServerSocketHandler implements Runnable {
		Socket clientSocket;
		public ServerSocketHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try {
				BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

				while (clientSocket.isConnected()) {
					String inputString = input.readLine();
					if (debug && !inputString.equals("keepalive")) System.out.println("DEBUG: Received message: " + inputString);

					if (inputString.split(" ")[0].matches("\\d+$")) {
						if (debug && !inputString.equals("keepalive")) System.out.println("DEBUG: Server message: " + inputString);
						//Server Request
						int requestServerID = Integer.parseInt(inputString.split(" ")[0]);
						int requestTimeStamp = Integer.parseInt(inputString.split(" ")[1]);
						String requestCommand = inputString.split(" ")[2];

						if (!(requestServerID == myID)) {
							switch (requestCommand) {
								case "REQUEST":
									int responseClock = Mutex.receiveRequest(new MutexRequest(requestServerID, requestTimeStamp, ""));
									String outputText = myID + " " + responseClock + " ACK\n";
									output.writeBytes(outputText);
									output.flush();
									if (debug) System.out.println("DEBUG: Sending output to server: " + outputText);
									break;
								case "RELEASE":
									String command = "";
									for (int i = 3; i < inputString.split(" ").length; i++) {
										command = command + inputString.split(" ")[i] + " ";
									}
									command = command.trim();

									Mutex.receiveRelease(new MutexRequest(requestServerID, requestTimeStamp, command));
									break;
							}
						}
						clientSocket.close();
					}
					else {
						if (debug && !inputString.equals("keepalive")) System.out.println("DEBUG: Client message: " + inputString);
						//Client Request
						if (!inputString.split(" ")[0].equals("keepalive")) {
							Mutex.sendRequest(inputString, output);
						}

						//always ack to ensure client does not think server goes down if cs takes longer than 100ms
						String outputText = "ack\n";
						output.writeBytes(outputText);
						output.flush();
						//if (debug) System.out.println("DEBUG: Sending output to client: " + outputText);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// REQUEST
	// ServerID ServerTimeStamp REQUEST
	// 0 1 REQUEST

	// ACK
	// ServerID ServerTimeStamp ACK
	// 0 1 ACK

	// RELEASE
	// ServerID ServerTimeStamp RELEASE Command
	// 0 1 RELEASE purchase Tim phone 10

	static class Mutex {
		private static int myTimestamp;
		private static PriorityQueue<MutexRequest> pq = new PriorityQueue<>();

		private synchronized static void sendRequest(String command, DataOutputStream outputStream) {
			myTimestamp = myTimestamp + 1;
			MutexRequest request = new MutexRequest(Server.myID, myTimestamp, command, outputStream);
			pq.add(request);
			if (ServerList.SendAllServersRequest() && pq.peek().ServerID == myID) {
				processNextRequest();
			}
		}

		private synchronized static void processNextRequest() {
			MutexRequest request = pq.poll();
			String response = ph.handleRequest(request.command);
			try {
				request.outputStream.writeBytes(response);
				request.outputStream.flush();
				request.outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			ServerList.SendAllServersRelease(request);
		}

		private synchronized static int receiveRequest(MutexRequest request) {
			pq.add(request);
			myTimestamp = Math.max(request.TimeStamp, myTimestamp)+1;
			return myTimestamp;
		}

		public synchronized static void receiveRelease(MutexRequest request) {
			myTimestamp = Math.max(request.TimeStamp, myTimestamp)+1;
			ph.handleRequest(request.command);
			pq.removeIf(r -> r.TimeStamp == request.TimeStamp && r.ServerID == request.ServerID);
			if (pq.peek().ServerID == myID) {
				processNextRequest();
			}
		}

		public synchronized static int getTimeStamp() {
			return myTimestamp;
		}
	}
}
