package client.ui;

import client.network.WebSocketClientEndpoint;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EditWindow extends JFrame {
    private final String filename;
    private JTextPane textPane;
    private boolean isUpdating = false;

    public EditWindow(String filename, String content) {
        this.filename = filename;

        setTitle(filename);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        textPane = new JTextPane();
        textPane.setText(content);
        JScrollPane scrollPane = new JScrollPane(textPane);

        add(scrollPane, BorderLayout.CENTER);
        setVisible(true);

        // ✅ 실시간 입력 동기화
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            private void sendUpdate() {
                if (isUpdating) return;

                String updatedContent = textPane.getText();
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "update_file");
                msg.addProperty("filename", filename);
                msg.addProperty("content", updatedContent);
                WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(msg.toString());
            }

            public void insertUpdate(DocumentEvent e) { sendUpdate(); }
            public void removeUpdate(DocumentEvent e) { sendUpdate(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        // ✅ 창이 닫힐 때 openEditors에서 제거
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                WebSocketClientEndpoint.removeEditor(filename);
            }
        });
    }

    public void updateContent(String newContent) {
        SwingUtilities.invokeLater(() -> {
            isUpdating = true;
            textPane.setText(newContent);
            isUpdating = false;
        });
    }
}
