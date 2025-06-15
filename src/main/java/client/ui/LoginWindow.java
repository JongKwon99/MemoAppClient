package client.ui;

import client.network.WebSocketClientEndpoint;

import javax.swing.*;
import java.awt.*;

public class LoginWindow extends JFrame {
    public LoginWindow() {
        setTitle("MemoApp - 접속");
        setSize(350, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 화면 중앙

        JLabel nicknameLabel = new JLabel("닉네임:");
        JTextField nicknameField = new JTextField();

        JLabel ipLabel = new JLabel("서버 주소:");
        JTextField ipField = new JTextField("ws://localhost:8080/ws");

        JButton connectButton = new JButton("접속");

        connectButton.addActionListener(e -> {
            String nickname = nicknameField.getText().trim();
            String serverIp = ipField.getText().trim();
            if (!nickname.isEmpty() && !serverIp.isEmpty()) {
                WebSocketClientEndpoint.connect(serverIp, nickname, this);  // 현재 JFrame 전달
            } else {
                JOptionPane.showMessageDialog(this, "닉네임과 서버 주소를 입력하세요");
            }
        });

        setLayout(new GridLayout(3, 2, 10, 10));
        add(nicknameLabel);
        add(nicknameField);
        add(ipLabel);
        add(ipField);
        add(new JLabel()); // 빈 칸
        add(connectButton);

        setVisible(true);
    }
}
