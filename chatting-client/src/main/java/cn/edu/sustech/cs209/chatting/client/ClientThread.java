package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import static java.lang.Math.min;

public class ClientThread implements Runnable{
    Socket socket;
    Controller controller;

    BufferedReader in;
    String info;


    ClientThread(Socket socket, Controller controller) throws IOException {
        this.socket = socket;
        this.controller = controller;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try{
            try{
                doService();
            } finally {
                in.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doService() {
        while (true) {
            try{
                info = in.readLine();
                if(info != null) {
                    synchronized (this) {
                        notify();
                    }
                    String[] s = info.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)",-1);
                    if(s[0].equals("Message")) {
                        // Privat Message like "Message,userA,uerB,content,timestamp"
                        // Room Message like "Message,userA,"userB,userC,...",content,timestamp"
                        String sendFrom = s[1];
                        String sendTo = s[2];
                        if(sendTo.charAt(0) == '"'){
                            sendTo = sendTo.substring(1, sendTo.length() - 1);
                        }
                        String content = s[3].substring(1, s[3].length() - 1);
                        Long timestamp = Long.parseLong(s[4]);
                        Message message = new Message(timestamp, sendFrom, sendTo, content);

                        // private chat, the room name for receiver should reverse
                        if(!sendTo.contains(",")) {
                            sendTo = sendFrom;
                        }
                        if(!controller.contents.containsKey(sendTo)) {
                            controller.contents.put(sendTo, FXCollections.observableArrayList());
                        }
                        controller.contents.get(sendTo).add(message);
                        String finalSendTo = sendTo;
                        Platform.runLater(() -> {
                            controller.chatContentList.getItems().clear();
                            controller.chatContentList.getItems().addAll(controller.contents.get(finalSendTo));
                            controller.currentRoom = finalSendTo;
                        });


                        if(!controller.chats.contains(sendTo)) {
                            controller.chats.add(sendTo);
                            String[] members = sendTo.split(",");
                            int len = members.length;
                            String title = "";
                            for(int i = 0; i < min(len, 3); i ++) {
                                title += members[i] + ",";
                            }
                            if(len > 1) title = title.substring(0, title.length() - 1) + "(" + String.valueOf(len) + ")";
                            else title = title.substring(0, title.length() - 1);
                            controller.roomTitles.add(title);
                            Platform.runLater(() -> {
                                controller.chatList.getItems().clear();
                                controller.chatList.getItems().addAll(controller.roomTitles);
                            });
                        }

                    }
                    else if(s[0].equals("LogOut")) {
                        String logOutUser = s[1];
                        System.out.println(logOutUser + " leaves");
                    }
                }
            } catch(IOException ioe) {
                System.out.println("Sever Disconnect");
            } catch(IllegalStateException ile) {
                System.out.println("New Message!");
            }
        }
    }
}
