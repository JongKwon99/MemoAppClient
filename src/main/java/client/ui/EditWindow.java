package client.ui;

import client.network.WebSocketClientEndpoint;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class EditWindow extends JFrame {
    private final String filename;
    private final List<JTextArea> lineEditors = new ArrayList<>();
    private final boolean[] isUpdating = new boolean[20]; // 20줄 대응

    public EditWindow(String filename, String content) {
        this.filename = filename;

        setTitle(filename);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String[] lines = content != null ? content.split("\n", -1) : new String[0];

        for (int i = 0; i < 20; i++) {
            JTextArea lineArea = new JTextArea();
            lineArea.setRows(1);
            lineArea.setLineWrap(false);
            lineArea.setWrapStyleWord(false);
            lineArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, lineArea.getPreferredSize().height));
            lineArea.setFocusTraversalKeysEnabled(false);

            int lineIndex = i;
            lineArea.getDocument().addDocumentListener(new DocumentListener() {
                private void sendUpdate() {
                    if (isUpdating[lineIndex]) return;

                    StringBuilder updatedContent = new StringBuilder();
                    for (int j = 0; j < 20; j++) {
                        updatedContent.append(lineEditors.get(j).getText());
                        if (j < 19) updatedContent.append("\n");
                    }

                    JsonObject msg = new JsonObject();
                    msg.addProperty("type", "update_file");
                    msg.addProperty("filename", filename);
                    msg.addProperty("content", updatedContent.toString());
                    WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(msg.toString());
                }

                public void insertUpdate(DocumentEvent e) { sendUpdate(); }
                public void removeUpdate(DocumentEvent e) { sendUpdate(); }
                public void changedUpdate(DocumentEvent e) {}
            });

            if (i < lines.length) {
                isUpdating[i] = true;
                lineArea.setText(lines[i]);
                isUpdating[i] = false;
            }

            lineEditors.add(lineArea);
            panel.add(lineArea);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                WebSocketClientEndpoint.removeEditor(filename);
            }
        });
    }

    public void updateContent(String newContent) {
        SwingUtilities.invokeLater(() -> {
            String[] lines = newContent != null ? newContent.split("\n", -1) : new String[0];
            for (int i = 0; i < 20; i++) {
                isUpdating[i] = true;
                lineEditors.get(i).setText(i < lines.length ? lines[i] : "");
                isUpdating[i] = false;
            }
        });
    }

    public void setEditable(int line, boolean editable) {
        if (line >= 0 && line < 20) {
            lineEditors.get(line).setEditable(editable);
        }
    }

    public void setLineColor(int line, Color color) {
        if (line >= 0 && line < 20) {
            lineEditors.get(line).setForeground(color);
        }
    }
}
