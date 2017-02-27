import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    
    public static void main (String[] args) {
        String hostAddress;
        int tcpPort;
        int udpPort;
        String mode="T";
        

        if (args.length != 3) {
            System.out.println("ERROR: Provide 3 arguments");
            System.out.println("\t(1) <hostAddress>: the address of the server");
            System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
            System.out.println("\t(3) <udpPort>: the port number for UDP connection");
            System.exit(-1);
        }

        hostAddress = args[0];
        tcpPort = Integer.parseInt(args[1]);
        udpPort = Integer.parseInt(args[2]);

        Scanner sc = new Scanner(System.in);
        /*
        while(sc.hasNextLine()) {
            String cmd = sc.nextLine();
            String[] tokens = cmd.split(" ");
            if (tokens[0].equals("setmode")) {
                // TODO: set the mode of communication for sending commands to the server 
                // and display the name of the protocol that will be used in future
            }
            else if (tokens[0].equals("purchase")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
            } else if (tokens[0].equals("cancel")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
            } else if (tokens[0].equals("search")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
            } else if (tokens[0].equals("list")) {
                // TODO: send appropriate command to the server and display the
                // appropriate responses form the server
            } else {
                System.out.println("ERROR: No such command");
            }
        }
        */
        while(sc.hasNextLine()) {
            String cmd = sc.nextLine();
            String[] tokens = cmd.split(" ");
            String response=null;
            if (tokens[0].equals("setmode")) {
                if(tokens[1].equals("T") || tokens[1].equals("U")){
                    mode=tokens[1];
                }

            }
            else if (tokens[0].equals("purchase") || tokens[0].equals("cancel")
                    || tokens[0].equals("search") || tokens[0].equals("list")) {
                try{
                    if (mode.equals("T"))
                        response=sendTcp(cmd,hostAddress,tcpPort);
                    else
                        response=sendUdp(cmd,hostAddress,udpPort);
                }catch(UnknownHostException e){
                    System.out.println("There was a problem with the specified host.");
                }catch(IOException e){
                    System.out.println("IOException in send command.");
                    e.printStackTrace();
                }
                System.out.println(response);
            } else {
                System.out.println("ERROR: No such command");
            }
        }     
    }
    
    private static String sendTcp(String cmd,String host,int port) throws UnknownHostException,IOException
    {
        Socket clientSocket = new Socket(host, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        outToServer.writeBytes(cmd + '\n');
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String response="";
        String newLine;
        do{
            newLine= inFromServer.readLine();
            System.out.println("DEBUG: "+newLine);
            response+=newLine;
        }while(newLine!=null);
        clientSocket.close();
        return response;
    }
    
    private static String sendUdp(String cmd, String host,int port) throws UnknownHostException,IOException
    {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName(host);
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        sendData = cmd.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String response = new String(receivePacket.getData());
        clientSocket.close();
        return response;
    }
}
