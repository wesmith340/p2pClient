package Packet;

import java.util.*;
import java.io.*;

/**
 * This class holds information to be sent over the internet
 */
public class Packet implements Serializable{
    //Declarations

    /*
      Event type determines how the packet should be handled
      EVENT_TYPE CODES
        INITIALIZE-> Client sending initialization info to server
        REQUEST-> Client is requesting file form server (wants to know which peer has it)
        REPLY-> Server is replying to client with peer id for file request
        QUIT_CLIENT-> Client wants to quit
        QUIT_SERVER-> Server wants to quit
    */
    public enum EVENT_TYPE {INITIALIZE, REQUEST, REPLY, QUIT_CLIENT, QUIT_SERVER}

    public int sender;  // sender id of packet
    public int recipient;  // recipient id of packet, not required to set for this project
    public EVENT_TYPE event_type; // see below for event type codes
    public int port_number;  // for reporting listening port number, can be found from attribute of socket
    public int fileIndex; //which file index is requested
    public String peerIP; // for telling the client the peer IP, not required to set for this project
    public int peerID; // will contain the id of the peer that contains the file
    public int peerListenPort; // will contain the listening port of the peer that contains the file.
    public char[] FILE_VECTOR; // contains the clients file_vector
    /*--------------------------------------------------------------------------------------------------------------------*/
    // Constructors
    /**
     * No-Arg Constructor
     */
    public Packet()
    {
        sender = -1;
        recipient = -1;
        event_type = null;
        port_number = -1;
        fileIndex = -1;
        peerIP = "";
        peerID = -1;
        peerListenPort = -1;
        FILE_VECTOR = new char[64];
        Arrays.fill(FILE_VECTOR, '0');
    }
    /**
     * Constructor
     * @param sender
     * @param recipient
     * @param event_type
     * @param port_number
     * @param fileIndex
     * @param peerIP
     * @param peerID
     * @param peerListenPort
     * @param FILE_VECTOR
     */
    public Packet(int sender, int recipient, EVENT_TYPE event_type, int port_number,
                  int fileIndex, String peerIP, int peerID, int peerListenPort, char[] FILE_VECTOR) {
        this.sender = sender;
        this.recipient = recipient;
        this.event_type = event_type;
        this.port_number = port_number;
        this.fileIndex = fileIndex;
        this.peerIP = peerIP;
        this.peerID = peerID;
        this.peerListenPort = peerListenPort;
        this.FILE_VECTOR = FILE_VECTOR;
    }
    /*--------------------------------------------------------------------------------------------------------------------*/
    // Packet building methods
    /**
     * This method builds a Packet to give the Server basic info about a Client
     * @param peerID
     * @param peerListenPort
     * @param FILE_VECTOR
     */
    public void initializationPacket(int peerID, int peerListenPort, char[] FILE_VECTOR) {
        this.event_type = EVENT_TYPE.INITIALIZE;
        this.peerID = peerID;
        this.peerListenPort = peerListenPort;
        this.FILE_VECTOR = FILE_VECTOR;
    }
    /**
     * This method builds a Packet to request the ID of a Client with a specific file
     * @param fileIndex
     */
    public void fileLookupPacket(int fileIndex) {
        this.event_type = EVENT_TYPE.REQUEST;
        this.fileIndex = fileIndex;
    }
    /**
     * This method builds a Packet to reply to a file lookup request
     * @param fileIndex
     * @param peerID
     */
    public void idReplyPacket(int fileIndex, int peerID) {
        this.event_type = EVENT_TYPE.REPLY;
        this.fileIndex = fileIndex;
        this.peerID = peerID;
    }
    /**
     * This method builds a Packet to request the Server close a Client Connection
     */
    public void ClientQuit() {
        this.event_type = EVENT_TYPE.QUIT_CLIENT;
    }
    /**
     * This method builds a Packet to tell a Client to close their connection
     */
    public void ServerQuit() {
        this.event_type = EVENT_TYPE.QUIT_SERVER;
    }
    /*--------------------------------------------------------------------------------------------------------------------*/
    // Miscellaneous methods
    /**
     * This method prints the contents of a Packet to the console
     */
    public void printPacket() {
        System.out.println("Packet Contents");
        System.out.println("---------------");
        System.out.println("Sender ID : "+sender);
        System.out.println("Receiver ID : "+recipient);
        System.out.println("Event Type : "+event_type);
        System.out.println("Port Number : "+port_number);
        System.out.println("Requested File Index : "+fileIndex);
        System.out.println("Peer IP : "+peerIP);
        System.out.println("Peer ID : "+peerID);
        System.out.println("Peer Listen Port : "+peerListenPort);
        System.out.println("File Vector : "+String.valueOf(FILE_VECTOR));
    }
    /*--------------------------------------------------------------------------------------------------------------------*/
}

