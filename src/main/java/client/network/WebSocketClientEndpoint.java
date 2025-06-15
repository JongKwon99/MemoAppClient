package client.network;

import client.ui.EditWindow;
import client.ui.MainWindow;
import com.google.gson.*;

import javax.swing.*;
import javax.websocket.*;
import java.net.URI;
import java.util.*;

@ClientEndpoint
public class WebSocketClientEndpoint {

    private static Session session;
    private static MainWindow mainWindow;
    private static JFrame loginWindow;
    private static String nickname;

    // 열려 있는 편집창 목록
    private static final Map<String, EditWindow> openEditors = new HashMap<>();

    public static void connect(String serverUri, String userNickname, JFrame loginWin) {
        try {
            nickname = userNickname;
            loginWindow = loginWin;

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(WebSocketClientEndpoint.class, new URI(serverUri + "?clientId=" + nickname));
        } catch (Exception e) {
            System.err.println("접속 실패: " + e.getMessage());
            JOptionPane.showMessageDialog(loginWindow, "서버 접속 실패: " + e.getMessage());
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        session = userSession;
        System.out.println("서버에 연결되었습니다.");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("서버 응답 수신: " + message);

        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();

            if (type.equals("file_list")) {
                JsonArray fileArray = json.getAsJsonArray("files");
                List<String> fileNames = new ArrayList<>();
                for (JsonElement elem : fileArray) {
                    fileNames.add(elem.getAsString());
                }

                SwingUtilities.invokeLater(() -> {
                    if (mainWindow == null) {
                        loginWindow.dispose();
                        mainWindow = new MainWindow(nickname);
                    }
                    mainWindow.updateFileList(fileNames);
                });

            } else if (type.equals("client_join")) {
                String nick = json.get("nickname").getAsString();
                System.out.printf("클라이언트 연결됨: %s%n", nick);
            } else if (type.equals("client_leave")) {
                String nick = json.get("nickname").getAsString();
                System.out.printf("클라이언트 연결 끊김: %s%n", nick);
            }
            else if (type.equals("file_content")) {
                String filename = json.get("filename").getAsString();
                String content = json.get("content").getAsString();

                SwingUtilities.invokeLater(() -> {
                    EditWindow existing = openEditors.get(filename);

                    if (existing == null || !existing.isDisplayable()) {  // ✨ isDisplayable() 체크 추가!
                        EditWindow editor = new EditWindow(filename, content);
                        openEditors.put(filename, editor);
                    }
                });
            } else if (type.equals("file_update_broadcast")) {
                String filename = json.get("filename").getAsString();
                String content = json.get("content").getAsString();

                EditWindow editor = openEditors.get(filename);
                if (editor != null) {
                    editor.updateContent(content);
                }
            } else if (type.equals("lock_state")) {
                String filename = json.get("filename").getAsString();
                JsonArray locks = json.getAsJsonArray("locks");

                Set<Integer> locked = new HashSet<>();
                for (JsonElement e : locks) {
                    JsonObject obj = e.getAsJsonObject();
                    int line = obj.get("line").getAsInt();
                    String lockedBy = obj.get("lockedBy").getAsString();
                    if (!lockedBy.equals(nickname)) {
                        locked.add(line);
                    }
                }
                EditWindow editor = openEditors.get(filename);
                if (editor != null) {
                    editor.updateLockedLines(locked);
                }
            }


        } catch (Exception e) {
            System.err.println("메시지 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("연결 종료: " + closeReason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        System.err.println("에러 발생: " + thr.getMessage());
        thr.printStackTrace();
    }

    public static Session getSession() {
        return session;
    }

    public static void removeEditor(String filename) {
        openEditors.remove(filename);
    }

}
