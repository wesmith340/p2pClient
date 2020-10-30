/*--------------------------------------------------------------------------------------------------------------------*/
/* Project 2                                                                                                          */
/* This application is designed to handle one Client in a Peer-to-Peer file sharing format                            */
/* @author SVSU - CS 401 - Weston Smith                                                                               */
/*--------------------------------------------------------------------------------------------------------------------*/

package p2pClient;

import Packet.Packet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This class is designed to handle client side operations for a Peer-to-Peer style file-sharer
 */
public class Client extends Thread{
    final Font font1     = new Font("Calibri", Font.PLAIN, 20);
    final Font messageFont = new Font("Courier New", Font.PLAIN, 20);
    private final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    private final int WIDTH = 1200;
    private final int HEIGHT = 600;

    private JFrame mainFrame;
    private static JTextArea outputArea;
    private JButton searchButton, listButton, quitButton;

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
        UIManager.put("OptionPane.messageFont", messageFont);
        UIManager.put("OptionPane.buttonFont", messageFont);


        mainFrame = new JFrame();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        outputArea = new JTextArea(30,30);
        outputArea.setEditable(false);
        outputArea.setFont(messageFont);
        outputArea.setBackground(Color.LIGHT_GRAY);
        outputArea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.add(outputArea);

        mainFrame.add(mainPanel);

        JPanel buttonPanel = new JPanel();
        searchButton = new JButton("Search for file");
        searchButton.setFont(font1);
        searchButton.addActionListener(new ButtonHandler());
        buttonPanel.add(searchButton);
        listButton = new JButton("List Connections");
        listButton.setFont(font1);
        listButton.addActionListener(new ButtonHandler());
        buttonPanel.add(listButton);
        quitButton = new JButton("Quit");
        quitButton.setFont(font1);
        quitButton.addActionListener(new ButtonHandler());
        buttonPanel.add(quitButton);
        mainFrame.add(buttonPanel, BorderLayout.SOUTH);

        mainFrame.setLocation((dim.width - WIDTH)/2, (dim.height - HEIGHT)/2);
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        WindowListener exitListener = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showOptionDialog(
                        null, "Are you sure to want to close the connection?",
                        "Exit Confirmation", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
                if (confirm == 0) {
                    closeConnection();
                    System.exit(0);
                }
            }
        };
        mainFrame.addWindowListener(exitListener);
        mainFrame.setMinimumSize(new Dimension(WIDTH, HEIGHT));


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
                outputArea.append("Successfully connected to the server\n");
                System.out.println("Successfully connected to the server");
                connected = true;
            }
        } catch (IOException e) {
            System.out.println("Unable to connect to the server");
            System.exit(0);
        }
        // If unable to connect, shutdown the client
        if (!connected) {
            System.out.println("Unable to connect to server, not caught");
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
            outputArea.append("Client " + fileHolderId + " has the file\n");
            System.out.println("Client " + fileHolderId + " has the file");
        } else {
            outputArea.append("No one has this file\n");
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
            //e.printStackTrace();
        }
        running = false;
        outputArea.append("Connection Closed\n");
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
                outputArea.append("Sent packet\n");
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
            outputArea.append("Closing the Connection\n");
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
    // Handlers
    private class ButtonHandler implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (event.getSource() == searchButton) {
                try {
                    int input = Integer.parseInt(JOptionPane.showInputDialog("Please enter the index of the file you are looking for"));
                    if (input >= 0 && input < 64) {
                        fileLookup(input);
                    } else {
                        JOptionPane.showMessageDialog(null, "Please enter a number between 0 and 63");
                    }
                } catch (NumberFormatException  e) {
                    JOptionPane.showMessageDialog(null, "Please enter a number between 0 and 63");
                }
            } else if (event.getSource() == listButton) {
                outputArea.append(String.format(
                        "Client ID: %d\nServer Port: %d\nClient Port: %d\nFile Vector: %s\n",
                        peerID, serverPort, peerListenPort, String.valueOf(FILE_VECTOR))
                );
            } else if (event.getSource() == quitButton) {
                int confirm = JOptionPane.showOptionDialog(
                        null, "Are you sure to want to close the connection?",
                        "Exit Confirmation", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
                if (confirm == 0) {
                    askToClose();
                    System.exit(0);
                }
            }
        }
    }
/*--------------------------------------------------------------------------------------------------------------------*/
    // Main
    /**
     * Main
     * @param args
     */
    public static void main(String args[]) {
        //-----------------------------------------------------------------------
        // File chooser
        final Font font1     = new Font("Calibri", Font.PLAIN, 20);
        final Font messageFont = new Font("Calibri", Font.PLAIN, 20);
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final int WIDTH  = 900, HEIGHT = 850;
        UIManager.put("OptionPane.messageFont", messageFont);
        UIManager.put("OptionPane.buttonFont", messageFont);

        JFileChooser fc = new JFileChooser();
        fc.setFont(font1);
        fc.setCurrentDirectory(new File("."));

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setSize(new Dimension(WIDTH, HEIGHT));
        frame.setLocation((dim.width - WIDTH)/2, (dim.height - HEIGHT)/2);
        frame.setResizable(true);
        File clientFile = null;
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            // Use JFileChooser to get a file
            clientFile = fc.getSelectedFile();
        } else {
            JOptionPane.showMessageDialog(null,
                    "ERROR: File not Found\nShutting down",
                    "Error Message", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        //-----------------------------------------------------------------------
        /*
        if (args.length < 2) {
            System.out.println("Usage: java Client Server_IP_Address Initialization_File_Name");
            System.exit(0);
        }

        File clientFile = new File(args[1]);
        */
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
        //String serverIP = args[0];
        String serverIP = JOptionPane.showInputDialog("Please enter the Server's IP address");

        // Instantiate the Client
        Client client = new Client(peerID, peerListenPort, FILE_VECTOR, serverIP, serverPort);
        System.out.println(client.toString());

        client.start();

        /*
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
    */
    }
/*--------------------------------------------------------------------------------------------------------------------*/
}
