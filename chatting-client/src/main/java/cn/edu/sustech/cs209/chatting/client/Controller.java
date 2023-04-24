package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.min;
import static java.net.IDN.toUnicode;

public class Controller implements Initializable {

    @FXML
    ListView<String> chatList;
    List<String> chats; // represents all the users in each room
    ObservableList<String> roomTitles; // represents the title of each room

    @FXML
    ListView<Message> chatContentList;
    Map<String, List<Message>> contents;

    @FXML
    Label currentUsername;
    String username;
    String currentRoom = null;

    @FXML
    Label currentOnlineCnt;
    String onlineCnt;

    @FXML
    TextArea inputArea;

    @FXML
    VBox emoticonBar;

    static final int port = 1234;
    Socket socket;
    PrintWriter out;

    ClientThread clientThread;
    Thread thread;


    public void connectToServer() {
        try {
            socket = new Socket("localhost", port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) ;
            clientThread = new ClientThread(socket, this);
            thread = new Thread(clientThread);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connectToServer();

        System.setProperty("file.encoding", "UTF-8");

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");


        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            String command = "NewUser," + input.get();
            synchronized (clientThread) {
                try {
                    out.println(command); // check if the name is duplicated from server, the command start with keyword "NewUser"
                    out.flush();
                    clientThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String response = clientThread.info;
                if(response.contains("Succeed")) {
                    username = input.get();
                    currentUsername.setText(username);
                    onlineCnt = response.substring(7);
                    currentOnlineCnt.setText(onlineCnt);
                } else {
                    command = "Exit"; // exit command
                    username = input.get();
                    System.out.println("User: " + username  +" have already logged in, exiting");
                    out.println(command);
                    out.flush();

                    try {
                        thread.stop();
                        out.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Platform.exit();
                }
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            out.println("Exit");
            out.flush();

            try {
                thread.stop();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Platform.exit();
        }

        chats = new ArrayList<>();
        roomTitles = FXCollections.observableArrayList();
        //chatList.setItems(roomTitles);
        chatList.setOnMouseClicked(mouseEvent -> {
            int i = chatList.getSelectionModel().getSelectedIndex();
            if(i <= chats.size() && i >= 0) {
                currentRoom = chats.get(i);
                chatContentList.getItems().clear();
                chatContentList.getItems().setAll(contents.get(currentRoom));
            }
        });

        contents = new HashMap<>();
        chatContentList.setCellFactory(new MessageCellFactory());

        Button button1 = new Button("\uD83D\uDE00");
        button1.setOnAction(actionEvent -> {
            inputArea.appendText(toUnicode("\uD83D\uDE00"));
        });
        Button button2 = new Button("\uD83D\uDE04");
        button2.setOnAction(actionEvent -> {
            inputArea.appendText(toUnicode("\uD83D\uDE04"));
        });
        Button button3 = new Button("\uD83D\uDE0A");
        button3.setOnAction(actionEvent -> {
            inputArea.appendText(toUnicode("\uD83D\uDE0A"));
        });
        emoticonBar.getChildren().addAll(button1, button2, button3);
    }

    @FXML
    public void createPrivateChat() {

        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // TODO: get the user list from server, the current user's name should be filtered out
        String command = "ShowAllUsers," + username;
        synchronized (clientThread) {
            try{
                out.println(command); // get all selectable users from server, the command starts with "ShowAllUsers"
                out.flush();
                clientThread.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String response = clientThread.info;
        String[] List = response.split(",");
        userSel.getItems().addAll(List);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        for(String friends : chats) {
            if(friends.equals(user.get())) {
                // set chatting window to the existing private room
                chatContentList.getItems().clear();
                chatContentList.getItems().addAll(contents.get(user.get()));
                currentRoom = user.get();
                return;
            }
        }

        // set chatting window to the new private room
        contents.put(user.get(), FXCollections.observableArrayList());
        chatContentList.getItems().clear();
        chatContentList.getItems().addAll(contents.get(user.get()));
        currentRoom = user.get();

        // the title of private room is the name of the selected user
        chats.add(user.get());
        roomTitles.add(user.get());
        chatList.getItems().clear();
        chatList.getItems().addAll(roomTitles);
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
        AtomicReference<String> user = new AtomicReference<>();
        Stage stage = new Stage();
        CheckBox[] usrSel = new CheckBox[10];
        VBox vBox = new VBox(20);

        String command = "ShowAllUsers," + username;
        synchronized (clientThread) {
            try{
                out.println(command); // get all selectable users from server, the command starts with "ShowAllUsers"
                out.flush();
                clientThread.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String response = clientThread.info;
        String[] List = response.split(",");
        for(int i = 0; i < List.length; i ++) {
            usrSel[i] = new CheckBox(List[i]);
            vBox.getChildren().add(usrSel[i]);
        }

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(getSelectedUsers(usrSel));
            stage.close();
        });

        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(20, 20, 20, 20));
        vBox.getChildren().add(okBtn);
        stage.setScene(new Scene(vBox));
        stage.showAndWait();

        for(String friends : chats) {
            if(friends.equals(user.get())) {
                // set chatting window to existing room
                chatContentList.getItems().clear();
                chatContentList.getItems().addAll(contents.get(user.get()));
                currentRoom = user.get();
                return;
            }
        }
        // set chatting window to new room
        contents.put(user.get(), FXCollections.observableArrayList());
        chatContentList.getItems().clear();
        chatContentList.getItems().addAll(contents.get(user.get()));
        currentRoom = user.get();

        // title of room is the first three members in the room
        String[] members = user.get().split(",");
        int len = members.length;
        String title = "";
        for(int i = 0; i < min(len, 3); i ++) {
            title += members[i] + ",";
        }
        title = title.substring(0, title.length() - 1) + "(" + String.valueOf(len) + ")";
        chats.add(user.get());
        roomTitles.add(title);
        chatList.getItems().clear();
        chatList.getItems().addAll(roomTitles);
    }

    String getSelectedUsers(CheckBox[] usrSel) {
        String[] selectedUsers = Arrays.stream(usrSel).filter(u -> u != null && u.isSelected()).map(u -> u.getText()).sorted((u1, u2) -> u1.compareTo(u2)).toArray(String[]::new);
        String ret = "";
        for(String u : selectedUsers) {
            ret += u + ",";
        }
        return ret.substring(0, ret.length() - 1);
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        // Privat Message like "Message,userA,uerB,content"
        // Room Message like "Message,userA,"userB,userC,...",content"
        String command = "Message" + "," + username + ",";
        if(currentRoom.contains(",")){
            command += "\"" + currentRoom + "\"" + ",";
        }else{
            command += currentRoom + ",";
        }
        String content = inputArea.getText();
        inputArea.clear();
        command += "\"" + content + "\"" + ",";
        Long timestamp = System.currentTimeMillis();
        command += timestamp;
        Message message = new Message(timestamp, username, currentRoom, content);
        contents.get(currentRoom).add(message);
        chatContentList.getItems().clear();
        chatContentList.getItems().addAll(contents.get(currentRoom));

        synchronized (clientThread) {
            out.println(command);
            out.flush();
        }

    }



    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
