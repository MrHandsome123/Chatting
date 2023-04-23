package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ChattingService implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Message message;
    private String currentUser;
    private Server server;

    ChattingService(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run(){
        try {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                doService();
            } finally {
                in.close();
                out.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doService() throws IOException {
        while(true){
            try{
                String command = in.readLine();
                if(command != null){
                    String[] s = command.split(",");
                    switch (s[0]){ // keyword of command
                        case "NewUser" :
                            if(server.clientList.stream().noneMatch(client -> client.currentUser.equals(s[1]))) {
                                currentUser = s[1];
                                out.println("Succeed");
                                out.flush();
                                server.clientList.add(this);
                            } else {
                                out.println("Fail");
                                out.flush();
                            }
                            break;
                        case "ShowAllUsers" :
                            String users = "";
                            for(ChattingService client : server.clientList) {
                                users += client.currentUser;
                                users += ",";
                            }
                            users = users.substring(0, users.length() - 1);
                            out.println(users);
                            out.flush();
                            break;
                        case "Exit" :
                            if(server.clientList.contains(this)){
                                server.clientList.remove(this);
                            }
                            out.println("Success");
                            out.flush();
                            break;
                    }
                }
            } catch (IOException ioe){
                ioe.printStackTrace();
                break;
            }
        }
    }
}