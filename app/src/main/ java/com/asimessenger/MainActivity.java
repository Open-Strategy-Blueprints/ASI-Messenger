package com.asimessenger;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button sendButton;
    private TextView statusText;
    private final Random random = new Random();
    private static final int PORT = 12345;
    private static final byte[] MAGIC = { 'A', 'S', 'I', '1' };
    private static final byte TTL = 6;
    private static final int SEND_COUNT = 10; // 每次发送到10个随机IP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        sendButton = findViewById(R.id.sendButton);
        statusText = findViewById(R.id.statusText);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = editText.getText().toString().trim();
                if (code.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入你的暗号", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendToUniverse(code);
            }
        });
    }

    private void sendToUniverse(String code) {
        statusText.setText("正在向宇宙广播...");
        sendButton.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] codeBytes = code.getBytes();
                if (codeBytes.length > 255) {
                    // 截断或警告，这里简单截断
                    byte[] truncated = new byte[255];
                    System.arraycopy(codeBytes, 0, truncated, 0, 255);
                    codeBytes = truncated;
                }

                // 构建消息包: MAGIC(4) + TTL(1) + length(1) + code
                byte[] packetData = new byte[4 + 1 + 1 + codeBytes.length];
                System.arraycopy(MAGIC, 0, packetData, 0, 4);
                packetData[4] = TTL;
                packetData[5] = (byte) codeBytes.length;
                System.arraycopy(codeBytes, 0, packetData, 6, codeBytes.length);

                int successCount = 0;
                try (DatagramSocket socket = new DatagramSocket()) {
                    for (int i = 0; i < SEND_COUNT; i++) {
                        InetAddress targetIp = generateRandomIp();
                        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, targetIp, PORT);
                        socket.send(packet);
                        successCount++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final int finalSuccessCount = successCount;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("已向 " + finalSuccessCount + " 个宇宙坐标发送暗号");
                        sendButton.setEnabled(true);
                        Toast.makeText(MainActivity.this, "你的信号已启程", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private InetAddress generateRandomIp() {
        byte[] ip = new byte[4];
        random.nextBytes(ip);
        // 避免多播地址 (224.0.0.0 - 239.255.255.255)
        // 简单处理：如果第一个字节在224-239之间，则重新生成
        while ((ip[0] & 0xFF) >= 224 && (ip[0] & 0xFF) <= 239) {
            random.nextBytes(ip);
        }
        try {
            return InetAddress.getByAddress(ip);
        } catch (Exception e) {
            // 不会发生
            return null;
        }
    }
}
