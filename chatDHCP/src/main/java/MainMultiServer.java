import java.util.Scanner;

import dhcp.client.DHCPClientNetworkMulti;
import dhcp.server.DHCPServerNetwork;

public class MainMultiServer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== PROJET DHCP MULTI-SERVEURS ===");
        System.out.println("1. Démarrer un serveur DHCP");
        System.out.println("2. Client DHCP multi-serveurs");
        System.out.println("3. Test avec plusieurs serveurs locaux");
        System.out.print("Votre choix: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                startSingleServer();
                break;
            case 2:
                startMultiServerClient();
                break;
            case 3:
                startMultiServerTest();
                break;
            default:
                System.out.println("Choix invalide");
        }
    }
    
    private static void startSingleServer() {
        try {
            DHCPServerNetwork server = new DHCPServerNetwork();
            server.startNetworkServer("chatDHCP/config/dhcp.properties");
        } catch (Exception e) {
            System.err.println("Erreur serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void startMultiServerClient() {
        DHCPClientNetworkMulti client = new DHCPClientNetworkMulti();
        client.startMultiServerInterface();
    }
    
    private static void startMultiServerTest() {
        try {
            System.out.println("Démarrage de plusieurs serveurs DHCP pour test...");
            
            // Démarrer 3 serveurs sur des ports différents
            for (int i = 0; i < 3; i++) {
                final int serverNum = i + 1;
                Thread serverThread = new Thread(() -> {
                    try {
                        System.out.println("Démarrage serveur " + serverNum);
                        // Ici vous pourriez configurer des serveurs avec des paramètres différents
                        DHCPServerNetwork server = new DHCPServerNetwork();
                        server.startNetworkServer("chatDHCP/config/dhcp.properties");
                    } catch (Exception e) {
                        System.err.println("Erreur serveur " + serverNum + ": " + e.getMessage());
                    }
                });
                serverThread.start();
                Thread.sleep(1000); // Délai entre les démarrages
            }
            
            Thread.sleep(3000); // Attendre que les serveurs démarrent
            
            // Démarrer le client multi-serveurs
            DHCPClientNetworkMulti client = new DHCPClientNetworkMulti();
            client.startMultiServerInterface();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du test multi-serveurs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}