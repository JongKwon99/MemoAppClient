package client.ui;

import client.network.WebSocketClientEndpoint;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainWindow extends JFrame {

    private JList<String> fileList;
    private DefaultListModel<String> listModel;

    public MainWindow(String nickname) {
        setTitle("MemoApp - 파일 목록 (" + nickname + ")");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(fileList);

        JButton addButton = new JButton("추가");
        JButton editButton = new JButton("편집");
        JButton deleteButton = new JButton("삭제");

        // [추가] 버튼 기능
        addButton.addActionListener(e -> {
            String newFile = JOptionPane.showInputDialog(this, "새 파일 이름 입력:");
            if (newFile != null && !newFile.trim().isEmpty()) {
                listModel.addElement(newFile.trim());

                // 서버에 새 파일 생성 요청 전송
                try {
                    javax.websocket.Session wsSession = client.network.WebSocketClientEndpoint.getSession();
                    if (wsSession != null && wsSession.isOpen()) {
                        com.google.gson.JsonObject request = new com.google.gson.JsonObject();
                        request.addProperty("type", "create_file");
                        request.addProperty("filename", newFile.trim());
                        wsSession.getAsyncRemote().sendText(request.toString());
                    }
                } catch (Exception ex) {
                    System.err.println("서버에 파일 생성 요청 중 오류: " + ex.getMessage());
                }
            }
        });

        // [편집] 버튼 기능
        editButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                JsonObject request = new JsonObject();
                request.addProperty("type", "read_file");
                request.addProperty("filename", selectedFile);
                WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(request.toString());
            } else {
                JOptionPane.showMessageDialog(this, "편집할 파일을 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            }
        });

        // [삭제] 버튼 기능
        deleteButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        selectedFile + " 파일을 정말 삭제하시겠습니까?",
                        "파일 삭제",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    JsonObject request = new JsonObject();
                    request.addProperty("type", "delete_file");
                    request.addProperty("filename", selectedFile);
                    WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(request.toString());
                }
            } else {
                JOptionPane.showMessageDialog(this, "삭제할 파일을 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            }
        });


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    public void updateFileList(List<String> files) {
        listModel.clear();
        for (String file : files) {
            listModel.addElement(file);
        }
    }
}
