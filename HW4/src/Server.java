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
		new Thread(new ServerListener(listenPort));

		ph = new ProductHandler();
		ph.readInventory(inventoryPath);

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
					clientSocket.setSoTimeout(100);
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

					if (inputString.split(" ")[0].matches("\\d+$")) {
						//Server Request
						int requestServerID = Integer.parseInt(inputString.split(" ")[0]);
						int requestTimeStamp = Integer.parseInt(inputString.split(" ")[1]);
						String requestCommand = inputString.split(" ")[2];

						switch (requestCommand) {
							case "REQUEST":
								int responseClock = Mutex.receiveRequest(new MutexRequest(requestServerID, requestTimeStamp, ""));
								output.writeBytes(myID + " " + responseClock + " ACK\n");
								output.flush();
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
						clientSocket.close();
					}
					else {
						//Client Request
						if (!inputString.split(" ").equals("keepalive")) {
							Mutex.sendRequest(inputString, output);
						}

						//always ack to ensure client does not think server goes down if cs takes longer than 100ms
						output.writeBytes("ack\n");
						output.flush();
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
