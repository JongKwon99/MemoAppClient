package client.ui;

import client.network.WebSocketClientEndpoint;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainWindow extends JFrame {

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> fileList = new JList<>(listModel);
    private final String nickname;

    public MainWindow(String nickname) {
        this.nickname = nickname;

        setTitle("MemoApp - 파일 목록 (" + nickname + ")");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JScrollPane scrollPane = new JScrollPane(fileList);

        JButton addButton = new JButton("추가");
        JButton editButton = new JButton("편집");
        JButton deleteButton = new JButton("삭제");

        // 추가 버튼: 새 파일 생성
        addButton.addActionListener(e -> {
            String newFile = JOptionPane.showInputDialog(this, "새 파일 이름 입력:");
            if (newFile != null && !newFile.trim().isEmpty()) {
                JsonObject request = new JsonObject();
                request.addProperty("type", "create_file");
                request.addProperty("filename", newFile.trim());
                WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(request.toString());
            }
        });

        // 편집 버튼: 파일 열기 요청
        editButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                JsonObject request = new JsonObject();
                request.addProperty("type", "read_file");
                request.addProperty("filename", selectedFile);
                WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(request.toString());
            }
        });

        // 삭제 버튼: 파일 삭제 요청
        deleteButton.addActionListener(e -> {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                JsonObject request = new JsonObject();
                request.addProperty("type", "delete_file");
                request.addProperty("filename", selectedFile);
                WebSocketClientEndpoint.getSession().getAsyncRemote().sendText(request.toString());

                listModel.removeElement(selectedFile);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
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
