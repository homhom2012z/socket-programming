import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class dataServer {
    //data_server
    private ServerSocket dataServerSocket;
    private Socket dataConnectionSocket;
    private PrintWriter output;
    private BufferedReader input;
    //authorize_server
    private DatagramSocket socketServer;
    private byte[] bufRec = new byte[256];
    private byte[] bufSend;
    private DatagramPacket packet;

    private int dataServerPort , autherizePort;
    private InetAddress address;
    private String serverIp = "127.0.0.1";

    private ArrayList<String[]> fileConfig = new ArrayList<String[]>();
    private ArrayList<String[]> dataList = new ArrayList<String[]>();


    public void runServer() throws IOException {

        //read_port_and_secret_keys
        File readConf = new File("server.config");
        Scanner confReader = new Scanner(readConf);

        while (confReader.hasNextLine()) {
            String[] confLines = confReader.nextLine().split("=");
            if(confLines[0].equals("data_server_port")){dataServerPort=Integer.valueOf(confLines[1]);}
            if(confLines[0].equals("authorize_server_port")){autherizePort=Integer.valueOf(confLines[1]);}
            fileConfig.add(confLines);
        }
        confReader.close();

        //read_user_and_passwords
        File readData_list = new File("data_list.csv");
        Scanner data_list = new Scanner(readData_list);
        data_list.nextLine();

        while (data_list.hasNextLine()) {
            String[] site_ip = data_list.nextLine().split(",");
            dataList.add(site_ip);
        }
        data_list.close();

        dataServerSocket = new ServerSocket(dataServerPort, 1);
        socketServer = new DatagramSocket(dataServerPort);

        System.out.println("Data Server Start");
        dataConnectionSocket = dataServerSocket.accept();

        while(true) {
            try{
                output = new PrintWriter(dataConnectionSocket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(dataConnectionSocket.getInputStream()));
            }catch (Exception e){
                dataConnectionSocket = dataServerSocket.accept();
                output = new PrintWriter(dataConnectionSocket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(dataConnectionSocket.getInputStream()));
            }

            try {
                String messageFromClient = input.readLine();
                System.out.println("Message : "+messageFromClient);
                String[] tokenSplit = messageFromClient.split(":");

                if(tokenSplit[1].equals("quit")){
                    System.out.println("SERVER DISCONNECT");
                    output.println("SERVER DISCONNECT");
                    dataServerSocket.close();
                    break;

                }else if(tokenSplit[1].equals("nametoip")||tokenSplit[1].equals("iptoname")) {
                    System.out.println("SENDING TO AUTHORIZE SERVER");
                    //SEND_TO_AUTHERIZE_SERVER
                    packet = new DatagramPacket(bufRec, bufRec.length);
                    String messageToServer = String.valueOf(tokenSplit[0]+":"+tokenSplit[1]+":"+tokenSplit[2]);
                    System.out.println(messageToServer);
                    address = InetAddress.getByName(serverIp);
                    bufSend = messageToServer.getBytes();
                    packet = new DatagramPacket(bufSend, bufSend.length, address, autherizePort);
                    socketServer.send(packet);
                    System.out.println("SENT TO AUTHERIZE SERVER");

                    //DATA_FROM_AUTHERIZE
                    packet = new DatagramPacket(bufRec, bufRec.length);
                    socketServer.receive(packet);
                    messageFromClient = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(messageFromClient);
                    String[] autherizeResponse = messageFromClient.split(":");
                    if(autherizeResponse[2].equals("true")){
                        String findingInfo = autherizeResponse[1];
                        boolean outputs = false;
                        String outputs_text = "";
                        for(String[] x: dataList){
                            if(autherizeResponse[0].equals("nametoip")){
                                if(findingInfo.equals(x[0])){
                                    outputs_text = x[1];
                                    outputs = true;
                                    break;
                                }
                            }else if(autherizeResponse[0].equals("iptoname")){
                                if(findingInfo.equals(x[1])){
                                    outputs_text = x[0];
                                    outputs = true;
                                    break;
                                }
                            }

                        }
                        if(outputs){
                            output.println(outputs_text);
                        }else{
                            output.println("not found");
                        }
                    }else{
                        output.close();
                        input.close();
                    }
                }else{
                    System.out.println("DISCONNECT : INVALID ACTION");
                    dataConnectionSocket.close();
                    //dataServerSocket = new ServerSocket(dataServerPort, 1);
                }
            }catch (Exception e){
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        dataServer authenServer = new dataServer();
        authenServer.runServer();
    }

}
