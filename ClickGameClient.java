import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class ClickGameClient {
    private static final String SERVER_ADDRESS = "10.49.10.242";
    private static final int SERVER_PORT = 12345;
    private static int player1Clicks = 0;
    private static int player2Clicks = 0;

    public static void main(String[] args) {
        // Kullanıcıdan nickname almak için geliştirilmiş giriş penceresi
        JTextField nicknameField = new JTextField(15);
        nicknameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nicknameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5))
        );

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(new Color(45, 45, 60));

        JLabel promptLabel = new JLabel("Adınızı girin:");
        promptLabel.setFont(new Font("Arial", Font.BOLD, 16));
        promptLabel.setForeground(new Color(200, 200, 200));
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        inputPanel.add(promptLabel);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(nicknameField);



        int option = JOptionPane.showConfirmDialog(null, inputPanel, "Tıklama Oyunu - Giriş", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION || nicknameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Geçerli bir ad girilmedi! Uygulama kapanıyor.", "Hata", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        String nickname = nicknameField.getText().trim();

        JFrame frame = new JFrame("Tıklama Oyunu - İstemci");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(45, 45, 60));

        JLabel timerLabel = new JLabel("Süre: Bekleniyor...");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        timerLabel.setForeground(new Color(220, 220, 220));
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel resultLabel = new JLabel("Sonuç: Bekleniyor...");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 18));
        resultLabel.setForeground(new Color(0, 200, 0));
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel player1ClicksLabel = new JLabel(nickname + " Tıklama Sayısı: 0");
        player1ClicksLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        player1ClicksLabel.setForeground(new Color(200, 200, 200));

        JLabel player2ClicksLabel = new JLabel("Diğer Oyuncu Tıklama Sayısı: 0");
        player2ClicksLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        player2ClicksLabel.setForeground(new Color(200, 200, 200));

        JButton clickButton = new JButton("Tıkla!");
        clickButton.setFont(new Font("Arial", Font.BOLD, 24));
        clickButton.setBackground(new Color(70, 130, 180));
        clickButton.setForeground(Color.WHITE);
        clickButton.setFocusPainted(false);
        clickButton.setEnabled(false);

        JPanel clickPanel = new JPanel();
        clickPanel.setBackground(new Color(45, 45, 60));
        clickPanel.add(clickButton);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBackground(new Color(45, 45, 60));
        infoPanel.add(player1ClicksLabel);
        infoPanel.add(player2ClicksLabel);

        frame.add(timerLabel, BorderLayout.NORTH);
        frame.add(clickPanel, BorderLayout.CENTER);
        frame.add(infoPanel, BorderLayout.WEST);
        frame.add(resultLabel, BorderLayout.SOUTH);

        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Nickname'i sunucuya gönderiyoruz
                out.println("NICKNAME:" + nickname);

                clickButton.addActionListener(e -> out.println("CLICK"));

                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("TIME:")) {
                        String[] parts = input.substring(5).split(" NICKNAME:");
                        String time = parts[0];
                        String opponentNickname = parts.length > 1 ? parts[1] : "";

                        SwingUtilities.invokeLater(() -> {
                            timerLabel.setText("Süre: " + time);
                            player1ClicksLabel.setText(nickname + " Tıklama Sayısı: " + player1Clicks);
                            player2ClicksLabel.setText(opponentNickname + " Tıklama Sayısı: " + player2Clicks);
                            clickButton.setEnabled(!time.equals("0"));
                        });
                    } else if (input.startsWith("CLICK_COUNT:")) {
                        String[] clickCounts = input.substring(12).split(",");
                        player1Clicks = Integer.parseInt(clickCounts[0]);
                        player2Clicks = Integer.parseInt(clickCounts[1]);

                    } else if (input.startsWith("RESULT:")) {
                        String result = input.substring(7);
                        SwingUtilities.invokeLater(() -> {
                            resultLabel.setText("Sonuç: " + result);
                            clickButton.setEnabled(false);
                        });
                        break;
                    }
                }

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Sunucuya bağlanılamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();

        frame.setVisible(true);
    }
}