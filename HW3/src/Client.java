//EIDS=KPP446,JC82563
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Scanner;

public class Client {
	public static final boolean debug = false;
	public static final int UDPPacketSize = 65507;
	public static Socket tcpSocket;
	public static DataOutputStream tcpSendStream;

	public static void main(String[] args) {
		String hostAddress;
		String mode = "T";

		//Argument Check
		if (args.length != 3) {
			System.out.println("ERROR: Provide 3 arguments");
			System.out.println("\t(1) <hostAddress>: the address of the server");
			System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
			System.out.println("\t(3) <udpPort>: the port number for UDP connection");
			System.exit(-1);
		}

		hostAddress = args[0];
		int tcpPort = Integer.parseInt(args[1]);
		int udpPort = Integer.parseInt(args[2]);
		try {
			ConnectToTCPSocket(hostAddress, tcpPort);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Scanner sc = new Scanner(System.in);
		while (sc.hasNextLine()) {
			String cmd = sc.nextLine();
			String[] tokens = cmd.split(" ");
			String response = null;
			if (tokens[0].equals("setmode")) {
				if (tokens[1].equals("T") || tokens[1].equals("U")) {
					mode = tokens[1];
					if (debug) System.out.println("DEBUG: Setting mode to: " + mode);
				} else {
					if (debug) System.out.println("DEBUG: Invalid command, correct syntax: setmode T|U");
				}
			} else if (tokens[0].equals("purchase") || tokens[0].equals("cancel")
					|| tokens[0].equals("search") || tokens[0].equals("list")) {
				try {
					if (mode.equals("T"))
						sendTcp(cmd, hostAddress, tcpPort);
					else
						response = sendUdp(cmd, hostAddress, udpPort);
				} catch (UnknownHostException e) {
					if (debug) System.out.println("There was a problem with the specified host.");
				} catch (IOException e) {
					if (debug) System.out.println("IOException in send command.");
					e.printStackTrace();
				}
				if (response != null) System.out.println(response);
			} else {
				System.out.println("ERROR: No such command");
			}
		}
	}

	private static void ConnectToTCPSocket(String host, int port) throws IOException {
		tcpSocket = new Socket(host, port);
		tcpSendStream = new DataOutputStream(tcpSocket.getOutputStream());
		new Thread(new TCPListener(tcpSocket)).start();
	}

	private static void sendTcp(String cmd, String host, int port) throws IOException {
		if (debug) System.out.println("DEBUG: Sending TCP Command: " + cmd);
		if (tcpSocket.isClosed()) {
			ConnectToTCPSocket(host, port);
		}
		tcpSendStream.writeBytes(cmd + '\n');
	}

	static class TCPListener implements Runnable {
		Socket socket;

		public TCPListener(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
				while (tcpSocket.isConnected()) {
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println(line.trim());
					}
				}
			} catch (IOException e) {
				if (debug) System.out.println("Error in TCPListener Receive: " + e.getStackTrace());
			}
		}
	}

	private static String sendUdp(String cmd, String host, int port) throws IOException {
		if (debug) System.out.println("DEBUG: Sending UDP Command: " + cmd);

		//Send
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress serverIP = InetAddress.getByName(host);
		byte[] sendData = cmd.getBytes();
		byte[] receiveData = new byte[UDPPacketSize];
		clientSocket.send(new DatagramPacket(sendData, sendData.length, serverIP, port));

		//Receive
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		String response = new String(receivePacket.getData()).trim();
		if (debug) System.out.println("DEBUG: UDP Received: " + response);
		clientSocket.close();

		return response;
	}
}
