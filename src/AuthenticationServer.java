import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

public class AuthenticationServer {
	
	private ServerSocket serverSocket;
    private Socket connectionSocket;
    private int AuthenPort;
    private PrintWriter output;
    private BufferedReader input;
    private ArrayList<String[]> fileConfig = new ArrayList<String[]>();
    private ArrayList<String[]> user_pass = new ArrayList<String[]>();
    
    public void runServer() throws IOException {
		//read_port_and_secret_keys
		File readConf = new File("server.config");
		Scanner confReader = new Scanner(readConf);
		
		while (confReader.hasNextLine()) {
			String[] confLines = confReader.nextLine().split("=");
			if(confLines[0].equals("authentication_server_port")){AuthenPort=Integer.valueOf(confLines[1]);}
			fileConfig.add(confLines);
		}
		//AuthenPort = Integer.valueOf(fileConfig.get(0)[1]);
		confReader.close();
		
    	//read_user_and_passwords
		File user_pass_read = new File("user_pass_action.csv");
		Scanner user_pass_reader = new Scanner(user_pass_read);
		user_pass_reader.nextLine();
		
	    while (user_pass_reader.hasNextLine()) {
	    	String[] user_password = user_pass_reader.nextLine().split(",");
	    	user_pass.add(user_password);
	    }
	    user_pass_reader.close();



    	int clientDisconnect = 3;

    	while(true) {
			serverSocket = new ServerSocket(AuthenPort, 1);
			System.out.println("Authenication Server is running");
			System.out.println("wait for client connected");
			connectionSocket = serverSocket.accept();
			//input_from_client
			output = new PrintWriter(connectionSocket.getOutputStream(), true);
			input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			System.out.println("Client connected");
			String user = input.readLine();
			String password = input.readLine();

			/*try{
				user = input.readLine();
				password = input.readLine();
			}catch (Exception e){
				//connectionSocket = serverSocket.accept();
			}*/

			//System.out.println("INPUT"+user+password);
			//Authentication
			boolean check = false;
			for(String[] x : user_pass) {
				if(user.split(":")[1].equals(x[0])&&password.split(":")[1].equals(x[1])) {
					check=true;
					break;
				}
			}
			if(check) {
				output.println(sendTokens(user.split(":")[1], password.split(":")[1]));
				//connectionSocket.close();
			}else {
				clientDisconnect-=1;
				output.println();
				if(clientDisconnect==0) {

					serverSocket.close();
					break;
				}
				output.println("null");
				//connectionSocket.close();
			}
			serverSocket.close();

    	}
    	
    }
    
    public String sendTokens(String user, String pass) {
    	String secretKey = "";
    	for(String[] x : fileConfig) {
    		if(x[0].equals("secret_key")) {secretKey = x[1]; break;}
    	}
    	String Input = user+"."+pass+"."+secretKey;
    	System.out.println("Input : "+Input);
    	String encoded = Base64.getEncoder().encodeToString(Input.getBytes());
    	System.out.println("Base64 : "+encoded);
    	return encoded;
    }

	public static void main(String[] args) throws IOException {
		AuthenticationServer authenServer = new AuthenticationServer();
		authenServer.runServer();

	}

}
