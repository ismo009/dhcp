import java.util.Scanner;

import dhcp.client.DHCPClient;
import dhcp.server.DHCPServer;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== PROJET DHCP - MENU PRINCIPAL ===");
        System.out.println("1. Démarrer le serveur DHCP");
        System.out.println("2. Test automatique (serveur + clients)");
        System.out.println("3. Mode client uniquement");
        System.out.println("Votre choix: ");
        
        int choice = scanner.nextInt();
        
        switch (choice) {
            case 1:
                startServer();
                break;
            case 2:
                runAutomaticTest();
                break;
            case 3:
                startClientMode();
                break;
            default:
                System.out.println("Choix invalide");
        }
    }
    
    private static void startServer() {
        try {
            DHCPServer server = new DHCPServer();
            server.initialize("chatDHCP/config/dhcp.properties");
            server.startCommandInterface();
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
        }
    }
    
    private static void runAutomaticTest() {
        try {
            System.out.println("\n=== TEST AUTOMATIQUE DU SYSTÈME DHCP ===\n");
            
            // Initialisation du serveur
            DHCPServer server = new DHCPServer();
            server.initialize("chatDHCP/config/dhcp.properties");
            
            // Affichage initial
            server.showAvailableIPs();
            
            // Test avec plusieurs clients
            for (int i = 1; i <= 3; i++) {
                System.out.println("--- Test avec Client " + i + " ---");
                DHCPClient client = new DHCPClient("CLIENT-" + i);
                
                boolean success = client.requestIPConfiguration(server);
                if (success) {
                    client.showConfiguration();
                } else {
                    System.out.println("Échec de configuration pour le client " + i);
                }
                
                Thread.sleep(1000); // Pause pour la lisibilité
            }
            
            // Affichage final
            server.showActiveLeases();
            server.showAvailableIPs();
            
            // Interface de commande
            server.startCommandInterface();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void startClientMode() {
        try {
            System.out.println("\n=== MODE CLIENT DHCP ===");
            System.out.println("Note: Assurez-vous qu'un serveur DHCP est déjà démarré");
            
            DHCPClient client = new DHCPClient();
            
            // Dans un cas réel, le client se connecterait via le réseau
            // Ici on simule avec un serveur local
            DHCPServer server = new DHCPServer();
            server.initialize("config/dhcp.properties");
            
            boolean success = client.requestIPConfiguration(server);
            if (success) {
                client.showConfiguration();
            } else {
                System.out.println("Échec de la configuration DHCP");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur en mode client: " + e.getMessage());
        }
    }
}