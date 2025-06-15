package client.network;

import client.ui.MainWindow;
import com.google.gson.*;

import javax.swing.*;
import javax.websocket.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ClientEndpoint
public class WebSocketClientEndpoint {

    private static Session session;
    private static MainWindow mainWindow;
    private static JFrame loginWindow;
    private static String nickname;

    public static void connect(String serverUri, String userNickname, JFrame loginWin) {
        try {
            nickname = userNickname;
            loginWindow = loginWin;

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(WebSocketClientEndpoint.class, new URI(serverUri + "?nickname=" + nickname));

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
            JsonElement parsed = JsonParser.parseString(message);
            if (!parsed.isJsonObject()) {
                System.err.println("⚠️ 수신한 메시지가 JSON 객체가 아님: " + parsed);
                return;
            }

            JsonObject json = parsed.getAsJsonObject();
            JsonElement typeElement = json.get("type");

            if (typeElement != null && typeElement.isJsonPrimitive() && typeElement.getAsJsonPrimitive().isString()) {
                String type = typeElement.getAsString();

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

                } else if (type.equals("file_content")) {
                    String filename = json.get("filename").getAsString();
                    String content = json.get("content").getAsString();

                    SwingUtilities.invokeLater(() -> {
                        new client.ui.EditWindow(filename, content);
                    });

                } else {
                    System.err.println("⚠️ 알 수 없는 메시지 타입: " + type);
                }
            } else {
                System.err.println("⚠️ type 필드가 문자열이 아님: " + typeElement);
            }
        } catch (Exception e) {
            System.err.println("메시지 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        System.err.println("에러 발생: " + thr.getMessage());
        thr.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("연결 종료: " + closeReason.getReasonPhrase());
    }

    public static Session getSession() {
        return session;
    }
}