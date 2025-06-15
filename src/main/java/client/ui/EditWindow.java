package client.ui;

import client.network.WebSocketClientEndpoint;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class EditWindow extends JFrame {

    private JTextArea textArea;
    private String filename;

    public EditWindow(String filename, String content) {
        this.filename = filename;

        setTitle("편집 - " + filename);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setText(content);
        JScrollPane scrollPane = new JScrollPane(textArea);

        // 🔄 실시간 동기화 - 텍스트 변경 시 서버로 전송
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                sendUpdateToServer();
            }

            public void removeUpdate(DocumentEvent e) {
                sendUpdateToServer();
            }

            public void changedUpdate(DocumentEvent e) {
                sendUpdateToServer(); // 스타일 변화 등
            }
        });

        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private void sendUpdateToServer() {
        String updatedContent = textArea.getText();

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "edit_file");
        msg.addProperty("filename", filename);
        msg.addProperty("content", updatedContent);

        WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(msg.toString());
    }
}
