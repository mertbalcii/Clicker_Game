import java.io.*; // Giriş/çıkış işlemleri için gerekli kütüphane
import java.net.*; // Ağ bağlantıları için gerekli kütüphane
import java.util.concurrent.atomic.AtomicInteger; // Eşzamanlı sayı işlemleri için

public class ClickGameServer {
    private static final int PORT = 12345; // Sunucu portu
    private static AtomicInteger player1Clicks = new AtomicInteger(0); // Oyuncu 1'in tıklama sayısı
    private static AtomicInteger player2Clicks = new AtomicInteger(0); // Oyuncu 2'nin tıklama sayısı
    private static volatile int timeRemaining = 130; // Oyun süresi (saniye cinsinden)
    private static volatile boolean gameRunning = false; // Oyunun çalışıp çalışmadığını kontrol eder
    private static String player1Nickname = ""; // Oyuncu 1'in takma adı
    private static String player2Nickname = ""; // Oyuncu 2'nin takma adı

    public static void main(String[] args) {
        System.out.println("Sunucu başlatılıyor...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // Sunucu soketi oluşturuluyor
            System.out.println("Sunucu " + PORT + " portunda çalışıyor.");

            // İki oyuncunun bağlanmasını bekliyoruz
            Socket player1Socket = serverSocket.accept(); // Oyuncu 1 bağlantısı
            System.out.println("Oyuncu 1 bağlandı.");
            Socket player2Socket = serverSocket.accept(); // Oyuncu 2 bağlantısı
            System.out.println("Oyuncu 2 bağlandı.");

            // Oyunculardan takma adlarını alıyoruz
            BufferedReader player1In = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
            BufferedReader player2In = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
            player1Nickname = player1In.readLine().split(":")[1]; // Gelen "NICKNAME:Ad" mesajından ad alınıyor
            player2Nickname = player2In.readLine().split(":")[1]; // Aynı işlem oyuncu 2 için

            // Her iki oyuncu için işlemci thread'leri başlatılıyor
            Thread player1Thread = new Thread(new PlayerHandler(player1Socket, 1)); // Oyuncu 1'in tıklama işleyicisi
            Thread player2Thread = new Thread(new PlayerHandler(player2Socket, 2)); // Oyuncu 2'nin tıklama işleyicisi

            player1Thread.start(); // Oyuncu 1 işleyici başlatılıyor
            player2Thread.start(); // Oyuncu 2 işleyici başlatılıyor

            // Oyunun başlatılması
            startGame(player1Socket, player2Socket);

        } catch (IOException e) {
            e.printStackTrace(); // Bağlantı hatalarını yazdırır
        }
    }

    // Oyunu başlatan ve kontrol eden metot
    private static void startGame(Socket player1Socket, Socket player2Socket) {
        gameRunning = true; // Oyun durumu aktif hale getiriliyor
        timeRemaining = 130; // Oyun süresi resetleniyor

        try {
            // Her iki oyuncuya mesaj göndermek için çıkış akışları
            PrintWriter player1Out = new PrintWriter(player1Socket.getOutputStream(), true);
            PrintWriter player2Out = new PrintWriter(player2Socket.getOutputStream(), true);

            // Geri sayım başlıyor
            while (timeRemaining > 0) {
                // Oyunculara kalan süre ve rakibin adını gönderiyoruz
                player1Out.println("TIME:" + timeRemaining + " NICKNAME:" + player2Nickname);
                player2Out.println("TIME:" + timeRemaining + " NICKNAME:" + player1Nickname);

                // Tıklama sayıları güncelleniyor
                String clickCounts = player1Clicks.get() + "," + player2Clicks.get();
                player1Out.println("CLICK_COUNT:" + clickCounts);
                player2Out.println("CLICK_COUNT:" + clickCounts);

                Thread.sleep(1000); // Her saniyede bir güncelleniyor
                timeRemaining--; // Geri sayım azaltılıyor
            }

            gameRunning = false; // Oyun sona erdi

            // Kazananı belirliyoruz
            String result;
            if (player1Clicks.get() > player2Clicks.get()) {
                result = "RESULT:" + player1Nickname + " Kazandı!";
            } else if (player2Clicks.get() > player1Clicks.get()) {
                result = "RESULT:" + player2Nickname + " Kazandı!";
            } else {
                result = "RESULT:Beraberlik!";
            }

            // Sonucu her iki oyuncuya gönderiyoruz
            player1Out.println(result);
            player2Out.println(result);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace(); // Hataları yazdırır
        }
    }

    // Her bir oyuncunun tıklama hareketlerini işleyen sınıf
    private static class PlayerHandler implements Runnable {
        private Socket socket; // Oyuncunun bağlı olduğu soket
        private int playerNumber; // Oyuncunun numarası (1 veya 2)

        public PlayerHandler(Socket socket, int playerNumber) {
            this.socket = socket;
            this.playerNumber = playerNumber;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String input;

                while ((input = in.readLine()) != null) { // Oyuncudan gelen mesajları dinle
                    if (input.equals("CLICK") && gameRunning) { // Gelen mesaj "CLICK" ve oyun devam ediyorsa
                        if (playerNumber == 1) {
                            player1Clicks.incrementAndGet(); // Oyuncu 1'in tıklama sayısı artırılıyor
                        } else if (playerNumber == 2) {
                            player2Clicks.incrementAndGet(); // Oyuncu 2'nin tıklama sayısı artırılıyor
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(); // Hataları yazdırır
            }
        }
    }
}
