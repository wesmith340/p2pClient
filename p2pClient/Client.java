/*--------------------------------------------------------------------------------------------------------------------*/
/* Project 2                                                                                                          */
/* This application is designed to handle one Client in a Peer-to-Peer file sharing format                            */
/* @author SVSU - CS 401 - Weston Smith                                                                               */
/*--------------------------------------------------------------------------------------------------------------------*/

package p2pClient;

import Packet.Packet;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This class is designed to handle client side operations for a Peer-to-Peer style file-sharer
 */
public class Client extends Thread{
    // Declarations
    protected int serverPort;
    protected InetAddress ip;
    protected Socket s;
    private ObjectOutputStream outputStream ;
    private ObjectInputStream inputStream ;
    protected int peerID;
    protected int peerListenPort;
    protected char FILE_VECTOR[];

    private boolean running = true;
/*--------------------------------------------------------------------------------------------------------------------*/
    // Constructors
    /**
     * No-Arg Constructor
     */
    public Client() {
        ip = null;
        peerID = -1;
        peerListenPort = -1;
        serverPort = -1;
        FILE_VECTOR = new char[64];
        Arrays.fill(FILE_VECTOR, '0');
    }

    /**
     * Constructor
     * @param peerID
     * @param peerListenPort
     * @param FILE_VECTOR
     * @param ip
     * @param serverPort
     */
    public Client(int peerID, int peerListenPort, char[] FILE_VECTOR, String ip, int serverPort) {
        this.peerID = peerID;
        this.peerListenPort = peerListenPort;
        this.FILE_VECTOR = FILE_VECTOR;
        connectToServer(ip, serverPort);
    }
    //---------------------------------------------------------------------
    // Methods used solely by Constructors
    /**
     * This method connects the client to the server
     */
    private void connectToServer(String ip, int serverPort) {
        boolean connected = false;
        try {
            s = new Socket(ip, serverPort);
            DataInputStream inputStream = new DataInputStream(s.getInputStream());
            if (inputStream.readBoolean()) {
                System.out.println("Successfully connected to the server");
                connected = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If unable to connect, shutdown the client
        if (!connected) {
            System.out.println("Unable to connect to the server");
            System.exit(0);
        }
    }
/*--------------------------------------------------------------------------------------------------------------------*/
    // Run method for threading
    /**
     * This method listens for incoming Packets to be handled
     */
    @Override
    public void run() {
        Packet iniPacket = new Packet();
        try {
            outputStream = new ObjectOutputStream(s.getOutputStream());
            inputStream = new ObjectInputStream(s.getInputStream());

            iniPacket.initializationPacket(peerID,peerListenPort,FILE_VECTOR);
            outputStream.writeObject(iniPacket);
            outputStream.flush();

            while (running) {
                eventHandler((Packet)inputStream.readObject());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error creating I/O stream");
            System.exit(0);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("p2pClient package does not contain this class");
        }
    }
/*--------------------------------------------------------------------------------------------------------------------*/
    // Methods handling incoming Packets
    /**
     * This method determines what the Packet is to be used for
     * @param packet
     */
    private void eventHandler(Packet packet) {
        switch (packet.event_type) {
            case REPLY : // Server is replying with to a file request
                fileLookupResult(packet.peerID);
                break;
            case QUIT_SERVER : // Server is closing the connection
                closeConnection();
                break;
        }
    }
    /**
     * This method outputs the results of the file lookup
     * @param fileHolderId
     */
    private void fileLookupResult(int fileHolderId) {
        if (fileHolderId >= 0) {
            System.out.println("Client " + fileHolderId + " has the file");
        } else {
            System.out.println("No one has this file");
        }
    }
    /**
     * This method closes the connection with the Server
     */
    public void closeConnection(){
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
        System.out.println("Connection Closed");
        System.exit(0);
    }
/*--------------------------------------------------------------------------------------------------------------------*/
    // Methods for sending Packets
    /**
     * This method builds a packet to be sent to the server. The packet contains the information
     * for the server to see which other clients have the file.
     * @param fileIndex
     */
    public void fileLookup(int fileIndex) {
        // This checks if the client already has the file
        if (FILE_VECTOR[fileIndex] == '0') {
            Packet fileLookupPacket = new Packet();
            fileLookupPacket.fileLookupPacket(fileIndex);
            try {
                outputStream.writeObject(fileLookupPacket);
                outputStream.flush();
                System.out.println("Sent packet");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error sending packet");
            }
        } else {
            System.out.println("You already have file "+fileIndex);
        }
    }
    /**
     * This method requests the server to close its connection
     */
    public void askToClose() {
        try {
            System.out.println("Closing the Connection");
            running = false;
            Packet closePacket = new Packet();
            closePacket.ClientQuit();
            outputStream.writeObject(closePacket);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error closing the connection");
            closeConnection();
        }
    }
/*--------------------------------------------------------------------------------------------------------------------*/
    // Miscellaneous methods
    /**
     * Getter for if the Client thread is running
     * @return Boolean
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * This method returns the value of the object in the form of a human-readable String
     * @return String
     */
    public String toString() {
        return String.format(
                "Client ID: %d\nServer Port: %d\nClient Port: %d\nFile Vector: %s",
                peerID, serverPort, peerListenPort, String.valueOf(FILE_VECTOR));
    }
/*--------------------------------------------------------------------------------------------------------------------*/
    // Main
    /**
     * Main
     * @param args
     */
    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Usage: java Client Server_IP_Address Initialization_File_Name");
            System.exit(0);
        }

        File clientFile = new File(args[1]);

        int peerID = -1, serverPort = -1, peerListenPort = -1;
        char[] FILE_VECTOR = null;
        try {
            // Read in the Client Configuration from the file
            Scanner fileInput = new Scanner(clientFile);
            while(fileInput.hasNext()) {
                String input = fileInput.next();
                if (input.equals("CLIENTID")) {
                    peerID = fileInput.nextInt();
                } else if (input.equals("SERVERPORT")) {
                    serverPort = fileInput.nextInt();
                } else if (input.equals("MYPORT")) {
                    peerListenPort = fileInput.nextInt();
                } else if (input.equals("FILE_VECTOR")) {
                    FILE_VECTOR = fileInput.next().toCharArray();
                } else {
                    System.out.println("Error parsing the file: " + input);
                }
            }
            fileInput.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found\nSHUTTING DOWN");
            System.exit(0);
        }

        // Instantiate the Client
        Client client = new Client(peerID, peerListenPort, FILE_VECTOR, args[0], serverPort);
        System.out.println(client.toString());

        client.start();

        //done! now loop for user input
        while (client.isRunning()){
            // User inputs
            System.out.println("Key codes :\n" +
                    "  f <- for file lookup\n" +
                    "  i <- for information on this Client\n" +
                    "  q <- to quit");
            Scanner scanner = new Scanner(System.in);
            char input = scanner.nextLine().toLowerCase().charAt(0);
            if (client.isRunning()) {


                if (input == 'f') {
                    System.out.println("Please enter the index of the file you are looking for");
                    int fileIndex = scanner.nextInt();
                    client.fileLookup(fileIndex);
                }else if (input == 'i') {
                    System.out.println(client.toString());
                } else if (input == 'q') {
                    client.askToClose();
                }
            }
            scanner.nextLine();

        }

    }
/*--------------------------------------------------------------------------------------------------------------------*/
}
