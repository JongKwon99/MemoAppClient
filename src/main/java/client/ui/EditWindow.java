package client.ui;

import client.network.WebSocketClientEndpoint;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class EditWindow extends JFrame {

    private static final Map<String, EditWindow> openWindows = new HashMap<>();
    private String filename;
    private JTextArea textArea;
    private boolean internalUpdate = false;

    public EditWindow(String filename, String content) {
        this.filename = filename;

        // 이미 열린 창이 있다면 포커스만 주고 return
        if (openWindows.containsKey(filename)) {
            openWindows.get(filename).requestFocus();
            return;
        }
        openWindows.put(filename, this);

        setTitle("편집: " + filename);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        textArea = new JTextArea(content);
        JScrollPane scrollPane = new JScrollPane(textArea);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            private void sendUpdate() {
                if (internalUpdate) return;

                String updatedContent = textArea.getText();
                JsonObject updateMessage = new JsonObject();
                updateMessage.addProperty("type", "edit_content");
                updateMessage.addProperty("filename", filename);
                updateMessage.addProperty("content", updatedContent);

                WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(updateMessage.toString());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                sendUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sendUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                sendUpdate();
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        setVisible(true);
    }

    public static void updateContentFromServer(String filename, String newContent) {
        EditWindow window = openWindows.get(filename);
        if (window != null) {
            window.internalUpdate = true;
            window.textArea.setText(newContent);
            window.internalUpdate = false;
        }
    }

    @Override
    public void dispose() {
        openWindows.remove(filename);
        super.dispose();
    }
}
