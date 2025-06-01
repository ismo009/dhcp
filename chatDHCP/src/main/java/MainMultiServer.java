import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
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
        System.out.println("4. Test de connectivité réseau");
        System.out.println("5. Diagnostics réseau");
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
            case 4:
                testNetworkConnectivity(scanner);
                break;
            case 5:
                networkDiagnostics();
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
    
    private static void testNetworkConnectivity(Scanner scanner) {
        System.out.print("Adresse IP à tester: ");
        String ip = scanner.nextLine();
        System.out.print("Port à tester (défaut 6767): ");
        String portStr = scanner.nextLine();
        int port = portStr.isEmpty() ? 6767 : Integer.parseInt(portStr);
        
        try {
            System.out.println("Test de connexion à " + ip + ":" + port + "...");
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 5000); // 5 secondes timeout
            System.out.println("✓ Connexion réussie !");
            socket.close();
        } catch (IOException e) {
            System.out.println("✗ Connexion échouée: " + e.getMessage());
        }
        
        // Test ping
        try {
            System.out.println("Test ping vers " + ip + "...");
            InetAddress address = InetAddress.getByName(ip);
            boolean reachable = address.isReachable(5000);
            System.out.println(reachable ? "✓ Ping réussi" : "✗ Ping échoué");
        } catch (IOException e) {
            System.out.println("✗ Ping échoué: " + e.getMessage());
        }
    }
    
    private static void networkDiagnostics() {
        System.out.println("\n=== DIAGNOSTICS RÉSEAU ===");
        
        try {
            // Afficher l'adresse IP locale
            System.out.println("--- Adresse IP locale ---");
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Nom d'hôte: " + localHost.getHostName());
            System.out.println("Adresse IP: " + localHost.getHostAddress());
            // Afficher toutes les interfaces réseau
            System.out.println("\n--- Interfaces réseau ---");
            Collections.list(NetworkInterface.getNetworkInterfaces()).forEach(ni -> {
                try {
                    if (ni.isUp() && !ni.isLoopback()) {
                        System.out.println("Interface: " + ni.getDisplayName());
                        Collections.list(ni.getInetAddresses()).forEach(addr -> {
                            if (addr instanceof java.net.Inet4Address) {
                                System.out.println("  IPv4: " + addr.getHostAddress());
                            }
                        });
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs d'interface
                }
            });
            
        } catch (Exception e) {
            System.err.println("Erreur lors du diagnostic: " + e.getMessage());
        }
    }
}