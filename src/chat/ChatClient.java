package chat;

import chat.constant.ChatConstants;
import chat.constant.ChatConstants.Config.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.*;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame(ChatConstants.Config.APP_CLIENT_NAME);
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    String currentUserName;

    // List Box
    JCheckBox chkPointToPoint = new JCheckBox("Broadcast Mode");
    JList<String> userList = new JList<>();
    DefaultListModel<String> listModel = new DefaultListModel<>();

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        userList.setModel(listModel);
        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Logged User List and Broadcast Mode
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(chkPointToPoint, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        frame.getContentPane().add(topPanel, BorderLayout.EAST);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        // Switch Messaging Mode
        chkPointToPoint.addActionListener(e -> {
            if (chkPointToPoint.isSelected()) {
                chkPointToPoint.setText(ChatConstants.Config.APP_STRING_P2P_MODE);
            } else {
                chkPointToPoint.setText(ChatConstants.Config.APP_STRING_BROADCAST_MODE);
            }
        });

        // where one client can send a message to a specific client. You can add some header to
        // the message to identify the recipient. You can get the receipient name from the listbox.
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                String input = textField.getText();

                // REMOVE ANY UNWANTED CHARACTERS USERS HAS ENTERED
                String message = sanitizeInput(input);

                if(chkPointToPoint.isSelected()){
                    // P2P mode
                    String recipient = String.join(",", userList.getSelectedValuesList());

                    if (!recipient.isEmpty()) {
                        // Can send a message to the selected user
                        System.out.println(ChatConstants.Event.E_P2P + recipient + ":" + message);
                        out.println(ChatConstants.Event.E_P2P + recipient + ":" + message);
                    }else {
                        JOptionPane.showMessageDialog(frame,
                                "Select a user from the list for P2P messaging.",
                                "No User Selected",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }else {
                    // Broadcast Mode
                    System.out.println(ChatConstants.Event.E_BROADCAST + message);
                    out.println(ChatConstants.Event.E_BROADCAST + message);
                }

                // Empty Text
                textField.setText("");

            }
        });


    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {
        // Make connection and initialize streams
        String serverAddress = "";
        while (true) {
            if (ChatConstants.Config.DebugActive) {
                // Make Easier the Debugging
                serverAddress = "127.0.0.1";
            } else {
                serverAddress = getServerAddress();
            }

            // EXIT THE APPLICATION
            if (serverAddress == null) {
                System.exit(0);
            }else if (serverAddress.isEmpty()) {
                continue;
            }

            // MAKE CONNECTION
            try {
                Socket socket = new Socket(serverAddress, 9001);
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                break;
            } catch (SocketException e) {
                System.out.println(e.getMessage());
                continue;
            }

        }

        // Process all messages from server, according to the protocol.
        while (true) {
            processEvents();
        }

    }

    /**
     * Process incoming Events from the server
     * @throws IOException - Throws Exception
     */
    private void processEvents() throws IOException {
        String line = in.readLine();

        if (line.startsWith(ChatConstants.Event.E_SUBMIT_NAME)) {
            // USER AUTHENTICATION
            this.currentUserName = getName();
            out.println(this.currentUserName);
        } else if (line.startsWith(ChatConstants.Event.E_NAME_ACCEPTED)) {
            // USER ACCEPT
            textField.setEditable(true);

        }else if (line.startsWith(ChatConstants.Event.E_USER_LIST)) {
            // UPDATE USER LIST
            String[] users = line.substring(9).split(",");
            listModel.clear();
            for (String user : users) {
                listModel.addElement(user);
            }
        } else if (line.startsWith(ChatConstants.Event.E_MESSAGE)) {
            // BROADCAST
            messageArea.append(line.substring(7) + "\n");

        } else if (line.startsWith(ChatConstants.Event.E_P2P)) {
            // Display P2P message
            messageArea.append(line.substring(3) + "\n");
        } else if (line.startsWith(ChatConstants.Event.E_FORCE_EXIT)) {
            // Force Exit The App
            System.exit(0);
        }
    }

    /**
     * Get Sanitized Text
     * Remove unwanted symbols and formatting
     */
    private static String sanitizeInput(String input) {
        return input.replaceAll("[:]", "");
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}