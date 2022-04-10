import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

public class AuthorizeServer {

    private int dataServerPort , autherizePort;

	private DatagramSocket socketServer;
    private byte[] bufRec = new byte[256];
    private byte[] bufSend;
    private DatagramPacket packet;
    private String message;

    private ArrayList<String[]> fileConfig = new ArrayList<String[]>();
    private ArrayList<String[]> user_pass = new ArrayList<String[]>();


    public void runServer() throws IOException {

        //file_part
        //read_port_and_secret_keys
        File readConf = new File("server.config");
        Scanner confReader = new Scanner(readConf);

        while (confReader.hasNextLine()) {
            String[] confLines = confReader.nextLine().split("=");
            if(confLines[0].equals("data_server_port")){dataServerPort=Integer.valueOf(confLines[1]);}
            if(confLines[0].equals("authorize_server_port")){autherizePort=Integer.valueOf(confLines[1]);}
            fileConfig.add(confLines);
        }
        //dataServerPort = Integer.valueOf(fileConfig.get(2)[1]);
        //autherizePort = Integer.valueOf(fileConfig.get(1)[1]);
        confReader.close();

        //read_user_and_passwords
        File user_pass_read = new File("user_pass_action.csv");
        Scanner user_pass_reader = new Scanner(user_pass_read);
        user_pass_reader.nextLine(); //skip_the_first_line

        while (user_pass_reader.hasNextLine()) {
            String[] user_password = user_pass_reader.nextLine().split(",");
            user_pass.add(user_password);
        }
        user_pass_reader.close();

        // Create udp server socket
        socketServer = new DatagramSocket(autherizePort);

        System.out.println("Autherize Server Start");

        while(true) {
            //RECEIVE_TOKEN&ACTIONS_FROM_DATA_SERVER
            try {
                packet = new DatagramPacket(bufRec, bufRec.length);
                socketServer.receive(packet);
                String messageFromDataServer = new String(packet.getData(), 0, packet.getLength());
                System.out.println(messageFromDataServer);

                String[] Tokens = messageFromDataServer.split(":");

                //DECODING
                byte[] decodedBytes = Base64.getDecoder().decode(Tokens[0]);
                String[] decodedString = new String(decodedBytes).split("\\.");

                String autherizedTokens = Tokens[1]+":"+Tokens[2]+":"+autherizeTokens(decodedString, Tokens);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                message = autherizedTokens;
                bufSend = message.getBytes();
                packet = new DatagramPacket(bufSend, bufSend.length, address, port);
                socketServer.send(packet);
                System.out.println("SENT TO DATA SERVER");

            }catch (Exception e){
                break;
            }

        }
    }

    public boolean autherizeTokens(String[] decodedString, String[] Token){
        String[] decode = decodedString;
        boolean autherizeTokens = false;
        for(String[] x: user_pass){
            if(decode[0].equals(x[0])&&decode[1].equals(x[1])){
                String[] checkAction = x[2].split(":");
                if(checkAction.length==2){
                    if(Token[1].equals(checkAction[0])||Token[1].equals(checkAction[1])){
                        autherizeTokens = true;
                        break;
                    }
                }else{
                    if(Token[1].equals(x[2])){
                        autherizeTokens = true;
                        break;
                    }
                }
            }
        }
        return autherizeTokens;
    }

	public static void main(String[] args) throws IOException {
        AuthorizeServer authenServer = new AuthorizeServer();
		authenServer.runServer();
	}

}
