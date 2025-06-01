import java.util.Scanner;

import dhcp.client.DHCPClientNetwork;
import dhcp.server.DHCPServerNetwork;

public class MainNetwork {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== PROJET DHCP RÉSEAU - MENU PRINCIPAL ===");
        System.out.println("1. Démarrer le serveur DHCP (sur cet ordinateur)");
        System.out.println("2. Se connecter comme client DHCP interactif");
        System.out.println("3. Test local automatique (serveur + client)");
        System.out.print("Votre choix: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consommer le retour à la ligne
        
        switch (choice) {
            case 1:
                startNetworkServer();
                break;
            case 2:
                startInteractiveClient(scanner);
                break;
            case 3:
                testLocalNetwork();
                break;
            default:
                System.out.println("Choix invalide");
        }
    }
    
    private static void startNetworkServer() {
        try {
            DHCPServerNetwork server = new DHCPServerNetwork();
            server.startNetworkServer("chatDHCP/config/dhcp.properties");
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void startInteractiveClient(Scanner scanner) {
        System.out.print("Adresse IP du serveur DHCP: ");
        String serverIP = scanner.nextLine().trim();
        
        System.out.print("ID du client (ou appuyez sur Entrée pour auto-générer): ");
        String clientId = scanner.nextLine().trim();
        
        try {
            DHCPClientNetwork client;
            if (clientId.isEmpty()) {
                client = new DHCPClientNetwork(serverIP);
            } else {
                client = new DHCPClientNetwork(clientId, serverIP);
            }
            
            // Lancer l'interface interactive
            client.startClientInterface();
            
        } catch (Exception e) {
            System.err.println("Erreur client: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testLocalNetwork() {
        try {
            System.out.println("Démarrage du test réseau local...");
            
            // Démarrer le serveur dans un thread séparé
            Thread serverThread = new Thread(() -> {
                try {
                    DHCPServerNetwork server = new DHCPServerNetwork();
                    server.startNetworkServer("chatDCHP/config/dhcp.properties");
                } catch (Exception e) {
                    System.err.println("Erreur serveur: " + e.getMessage());
                }
            });
            serverThread.start();
            
            // Attendre que le serveur démarre
            Thread.sleep(2000);
            
            // Créer et lancer un client interactif
            DHCPClientNetwork client = new DHCPClientNetwork("localhost");
            client.startClientInterface();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}