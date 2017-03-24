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

		while (sc.hasNextLine()) {
			String cmd = sc.nextLine();
			String[] tokens = cmd.split(" ");

			switch (tokens[0]) {
				case "purchase":
				case "cancel":
				case "search":
				case "list":
					while (!cn.Send(cmd));
					break;
				default:
					System.out.println("ERROR: No such command");
			}
		}
	}

	static class ClientNetworking {
		static ArrayList<URI> serverList;
		static int serverToUse;

		public ClientNetworking(ArrayList<URI> servers) {
			serverList = servers;
			serverToUse = 0;
		}

		static synchronized boolean Send(String cmd) throws IOException {

			Socket clientSocket;
			BufferedReader reader;
			DataOutputStream outStream;

			try {
				clientSocket = new Socket();
				if (debug) System.out.println("DEBUG: Trying to connect to server: " + serverToUse);
				if (debug) System.out.println("DEBUG: Host: " + serverList.get(serverToUse).getHost());
				if (debug) System.out.println("DEBUG: Port: " + serverList.get(serverToUse).getPort());
				clientSocket.connect(new InetSocketAddress(serverList.get(serverToUse).getHost(), serverList.get(serverToUse).getPort()), timeout);
				if (debug) System.out.println("DEBUG: Connected to server: " + serverToUse);
				clientSocket.setSoTimeout(timeout);
				if (debug) System.out.println("DEBUG: Setting input reader on server: " + serverToUse);
				reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				if (debug) System.out.println("DEBUG: Setting output reader on server: " + serverToUse);
				outStream = new DataOutputStream(clientSocket.getOutputStream());
			} catch (IOException e) {
				if (debug) System.out.println("DEBUG: Error in TCPListener.Connect: " + e.getStackTrace());
				serverToUse = (serverToUse + 1) % serverList.size();
				return false;
			}

			outStream.writeBytes(cmd + '\n');
			outStream.flush();

			String line;
			StringBuilder output = new StringBuilder();
			try {
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.equals("end")) {
						System.out.println(output.toString().trim());
						clientSocket.close();
						return true;
					}
					else {
						output.append(line+"\n");
					}
				}
			} catch (IOException e) {
				if (debug)
					System.out.println("DEBUG: Error in TCPListener Receive, trying new server: " + e.getMessage());
			}
			return false;
		}
	}
}
