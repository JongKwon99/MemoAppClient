package client.ui;

import client.network.WebSocketClientEndpoint;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class EditWindow extends JFrame {
    private final String filename;
    private final List<JTextArea> lineEditors = new ArrayList<>();
    private final boolean[] isUpdating = new boolean[20];
    private final Set<Integer> lockedLines = new HashSet<>();
    private int currentFocusedLine = -1;

    public EditWindow(String filename, String content) {
        this.filename = filename;

        setTitle(filename);
        setSize(600, 600);
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

            // 입력 감지
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

            // 포커스 감지 → 커서 이동 요청
            lineArea.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (lockedLines.contains(lineIndex)) {
                        // 잠긴 줄이면 이동 못하게 하고, 포커스 복구
                        SwingUtilities.invokeLater(() -> {
                            if (currentFocusedLine >= 0 && currentFocusedLine < 20) {
                                lineEditors.get(currentFocusedLine).requestFocusInWindow();
                            } else {
                                lineArea.transferFocus();  // 임의 이동
                            }
                        });
                        return;
                    }

                    if (currentFocusedLine != lineIndex) {
                        currentFocusedLine = lineIndex;

                        JsonObject move = new JsonObject();
                        move.addProperty("type", "cursor_move");
                        move.addProperty("filename", filename);
                        move.addProperty("line", lineIndex);
                        WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(move.toString());
                    }
                }
            });

            // 🔒 키 입력 차단
            lineArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if (lockedLines.contains(lineIndex)) e.consume();
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (lockedLines.contains(lineIndex)) e.consume();
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (lockedLines.contains(lineIndex)) e.consume();
                }
            });

            // 초기 텍스트 설정
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

        // 창 닫힐 때 lock 해제
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                WebSocketClientEndpoint.removeEditor(filename);
                if (currentFocusedLine != -1) {
                    JsonObject unlock = new JsonObject();
                    unlock.addProperty("type", "cursor_move");
                    unlock.addProperty("filename", filename);
                    unlock.addProperty("line", -1); // 해제 의미
                    WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(unlock.toString());
                }
            }
        });
    }

    // 서버로부터 전체 내용 업데이트 시 사용
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

    // 서버로부터 받은 잠금 상태 반영
    public void updateLockedLines(Set<Integer> locked) {
        SwingUtilities.invokeLater(() -> {
            lockedLines.clear();
            lockedLines.addAll(locked);

            for (int i = 0; i < 20; i++) {
                if (locked.contains(i)) {
                    lineEditors.get(i).setEditable(false);
                    lineEditors.get(i).setForeground(Color.RED);
                } else {
                    lineEditors.get(i).setEditable(true);
                    lineEditors.get(i).setForeground(Color.BLACK);
                }
            }
        });
    }
}
