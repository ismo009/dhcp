package dhcp.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dhcp.messages.DHCPMessage;
import dhcp.messages.DiscoverMessage;
import dhcp.messages.RequestMessage;

/**
 * Serveur DHCP avec support réseau
 */
public class DHCPServerNetwork extends DHCPServer {
    private ServerSocket serverSocket;
    private ExecutorService clientHandlerPool;
    private boolean running = false;
    private int port = 67; // Port DHCP standard (ou utilisez 6767 pour les tests)
    
    public DHCPServerNetwork() {
        super();
        this.clientHandlerPool = Executors.newFixedThreadPool(10);
    }
    
    /**
     * Démarre le serveur réseau
     */
    public void startNetworkServer(String configFile) throws Exception {
        // Initialiser la configuration
        initialize(configFile);
        
        // Utiliser un port alternatif pour les tests (pas besoin de droits admin)
        port = 6767;
        
        // Détecter automatiquement l'adresse IP locale
        String serverIP = getLocalNetworkIP();
        System.out.println("Adresse IP détectée automatiquement: " + serverIP);
        
        // Utiliser cette IP au lieu de l'IP codée en dur
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(serverIP));
    
        System.out.println("Serveur DHCP démarré sur le port " + port);
        System.out.println("Adresse du serveur: " + serverIP);
        System.out.println("En attente de clients...\n");
        
        // Thread pour l'interface de commande
        new Thread(this::startCommandInterface).start();
        
        // Boucle d'écoute des clients
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientHandlerPool.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur lors de l'acceptation du client: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gère un client connecté
     */
    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
            
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            System.out.println("Client connecté depuis: " + clientIP);
            
            // Lire le message du client
            DHCPMessage message = (DHCPMessage) in.readObject();
            DHCPMessage response = null;
            
            switch (message.getMessageType()) {
                case DISCOVER:
                    response = handleDiscover((DiscoverMessage) message);
                    break;
                case REQUEST:
                    response = handleRequest((RequestMessage) message);
                    break;
                default:
                    System.out.println("Type de message non supporté: " + message.getMessageType());
            }
            
            // Envoyer la réponse
            if (response != null) {
                out.writeObject(response);
                out.flush();
                System.out.println("Réponse envoyée au client " + clientIP);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
            }
        }
    }
    
    /**
     * Interface de commande modifiée pour le serveur réseau
     */
    @Override
    public void startCommandInterface() {
        Scanner scanner = new Scanner(System.in);
        String command;
        
        System.out.println("\n=== SERVEUR DHCP RÉSEAU - INTERFACE DE COMMANDE ===");
        System.out.println("Commandes disponibles:");
        System.out.println("  'available' - Afficher les adresses IP disponibles");
        System.out.println("  'leases' - Afficher les baux actifs");
        System.out.println("  'clean' - Nettoyer les baux expirés");
        System.out.println("  'status' - Statut du serveur");
        System.out.println("  'quit' - Arrêter le serveur");
        System.out.println("======================================================\n");
        
        while (running) {
            System.out.print("DHCP-Server> ");
            command = scanner.nextLine().trim().toLowerCase();
            
            switch (command) {
                case "available":
                    showAvailableIPs();
                    break;
                case "leases":
                    showActiveLeases();
                    break;
                case "clean":
                    ipPool.cleanExpiredLeases();
                    System.out.println("Nettoyage des baux expirés effectué.\n");
                    break;
                case "status":
                    showServerStatus();
                    break;
                case "quit":
                    stopServer();
                    return;
                default:
                    System.out.println("Commande inconnue.\n");
            }
        }
    }
    
    /**
     * Affiche le statut du serveur
     */
    private void showServerStatus() {
        try {
            System.out.println("\n=== STATUT DU SERVEUR ===");
            System.out.println("Adresse IP: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Port: " + port);
            System.out.println("État: " + (running ? "Actif" : "Arrêté"));
            System.out.println("Clients actifs: " + ipPool.getActiveLeases().size());
            System.out.println("IPs disponibles: " + ipPool.getAvailableIPs().size());
            System.out.println("========================\n");
        } catch (UnknownHostException e) {
            System.err.println("Erreur lors de l'obtention de l'adresse IP: " + e.getMessage());
        }
    }
    
    /**
     * Arrête le serveur réseau
     */
    private void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientHandlerPool.shutdown();
            shutdown();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arrêt du serveur: " + e.getMessage());
        }
    }
    
    private String getLocalNetworkIP() throws Exception {
        // Obtenir toutes les interfaces réseau
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (ni.isUp() && !ni.isLoopback()) {
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        // Préférer les adresses du réseau local
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }
}