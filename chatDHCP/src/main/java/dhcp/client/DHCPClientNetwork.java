package dhcp.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Scanner;

import dhcp.messages.AckMessage;
import dhcp.messages.DHCPMessage;
import dhcp.messages.DiscoverMessage;
import dhcp.messages.OfferMessage;
import dhcp.messages.RequestMessage;

/**
 * Client DHCP avec support réseau et interface interactive
 */
public class DHCPClientNetwork extends DHCPClient {
    private String serverAddress;
    private int serverPort = 6767; // Port pour les tests
    private Scanner scanner;
    private boolean connected = false;
    
    public DHCPClientNetwork(String serverAddress) {
        super();
        this.serverAddress = serverAddress;
        this.scanner = new Scanner(System.in);
    }
    
    public DHCPClientNetwork(String clientId, String serverAddress) {
        super(clientId);
        this.serverAddress = serverAddress;
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Interface interactive du client
     */
    public void startClientInterface() {
        System.out.println("\n=== CLIENT DHCP - INTERFACE INTERACTIVE ===");
        System.out.println("Client ID: " + getClientId());
        System.out.println("Serveur cible: " + serverAddress + ":" + serverPort);
        System.out.println("============================================");
        
        while (true) {
            showMenu();
            int choice = getMenuChoice();
            
            switch (choice) {
                case 1:
                    testConnection();
                    break;
                case 2:
                    sendDiscoverOnly();
                    break;
                case 3:
                    requestFullConfiguration();
                    break;
                case 4:
                    showCurrentConfiguration();
                    break;
                case 5:
                    renewLease();
                    break;
                case 6:
                    changeServer();
                    break;
                case 7:
                    showClientInfo();
                    break;
                case 8:
                    System.out.println("Au revoir !");
                    return;
                default:
                    System.out.println("Choix invalide. Veuillez réessayer.\n");
            }
        }
    }
    
    /**
     * Affiche le menu des options
     */
    private void showMenu() {
        System.out.println("\n=== MENU CLIENT DHCP ===");
        System.out.println("1. Tester la connexion au serveur");
        System.out.println("2. Envoyer DISCOVER seulement");
        System.out.println("3. Demander configuration IP complète");
        System.out.println("4. Afficher ma configuration actuelle");
        System.out.println("5. Renouveler le bail (si configuré)");
        System.out.println("6. Changer de serveur");
        System.out.println("7. Informations du client");
        System.out.println("8. Quitter");
        System.out.print("Votre choix (1-8): ");
    }
    
    /**
     * Lit le choix de l'utilisateur
     */
    private int getMenuChoice() {
        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consommer le retour à la ligne
            return choice;
        } catch (Exception e) {
            scanner.nextLine(); // Nettoyer le buffer
            return -1;
        }
    }
    
    /**
     * Teste la connexion au serveur
     */
    private void testConnection() {
        System.out.println("\n--- Test de connexion ---");
        log("Test de connexion au serveur " + serverAddress + ":" + serverPort);
        
        if (testServerConnection()) {
            connected = true;
            System.out.println("✓ Connexion réussie au serveur DHCP");
        } else {
            connected = false;
            System.out.println("✗ Impossible de se connecter au serveur DHCP");
            System.out.println("  Vérifiez que:");
            System.out.println("  - Le serveur est démarré");
            System.out.println("  - L'adresse IP est correcte");
            System.out.println("  - Le port 6767 est ouvert");
        }
        pauseAndContinue();
    }
    
    /**
     * Envoie uniquement un message DISCOVER
     */
    private void sendDiscoverOnly() {
        System.out.println("\n--- Envoi DISCOVER uniquement ---");
        
        if (!checkConnection()) return;
        
        try {
            DiscoverMessage discover = new DiscoverMessage(getClientId());
            log("Envoi DISCOVER...");
            
            DHCPMessage response = sendMessageToServer(discover);
            
            if (response instanceof OfferMessage) {
                OfferMessage offer = (OfferMessage) response;
                System.out.println("✓ OFFER reçu:");
                System.out.println("  IP proposée: " + offer.getOfferedIP().getHostAddress());
                System.out.println("  Masque: " + offer.getSubnetMask().getHostAddress());
                System.out.println("  Passerelle: " + offer.getDefaultGateway().getHostAddress());
                System.out.println("  DNS: " + offer.getDnsServer().getHostAddress());
                System.out.println("  Durée bail: " + offer.getLeaseDuration() + " secondes");
                
                System.out.print("\nVoulez-vous accepter cette offre ? (o/n): ");
                String accept = scanner.nextLine().trim().toLowerCase();
                
                if (accept.equals("o") || accept.equals("oui")) {
                    acceptOffer(offer);
                } else {
                    System.out.println("Offre refusée.");
                }
            } else {
                System.out.println("✗ Réponse inattendue: " + response.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            log("ERREUR lors de l'envoi DISCOVER: " + e.getMessage());
        }
        
        pauseAndContinue();
    }
    
    /**
     * Accepte une offre reçue
     */
    private void acceptOffer(OfferMessage offer) {
        try {
            RequestMessage request = new RequestMessage(
                getClientId(),
                offer.getOfferedIP(),
                offer.getServerAddress()
            );
            log("Envoi REQUEST pour accepter l'offre...");
            
            DHCPMessage response = sendMessageToServer(request);
            
            if (response instanceof AckMessage) {
                AckMessage ack = (AckMessage) response;
                if (handleAck(ack)) {
                    System.out.println("✓ Configuration IP acceptée et appliquée!");
                } else {
                    System.out.println("✗ Erreur lors de l'application de la configuration");
                }
            } else {
                System.out.println("✗ ACK attendu, reçu: " + response.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            log("ERREUR lors de l'acceptation de l'offre: " + e.getMessage());
        }
    }
    
    /**
     * Demande une configuration IP complète (processus DHCP complet)
     */
    private void requestFullConfiguration() {
        System.out.println("\n--- Demande de configuration IP complète ---");
        
        if (!checkConnection()) return;
        
        log("Démarrage du processus DHCP complet...");
        boolean success = requestIPConfigurationNetwork();
        
        if (success) {
            System.out.println("✓ Configuration IP obtenue avec succès!");
            showCurrentConfiguration();
        } else {
            System.out.println("✗ Échec de l'obtention de la configuration IP");
        }
        
        pauseAndContinue();
    }
    
    /**
     * Affiche la configuration actuelle
     */
    private void showCurrentConfiguration() {
        System.out.println("\n--- Configuration actuelle ---");
        showConfiguration();
        pauseAndContinue();
    }
    
    /**
     * Renouvelle le bail actuel
     */
    private void renewLease() {
        System.out.println("\n--- Renouvellement du bail ---");
        
        if (!hasValidConfiguration()) {
            System.out.println("✗ Aucune configuration IP active à renouveler");
            System.out.println("  Veuillez d'abord obtenir une configuration IP");
            pauseAndContinue();
            return;
        }
        
        if (!checkConnection()) return;
        
        try {
            // Envoyer une nouvelle demande REQUEST avec l'IP actuelle
            RequestMessage request = new RequestMessage(
                getClientId(),
                getAssignedIP(),
                null // Le serveur sera déterminé automatiquement
            );
            log("Demande de renouvellement du bail...");
            
            DHCPMessage response = sendMessageToServer(request);
            
            if (response instanceof AckMessage) {
                AckMessage ack = (AckMessage) response;
                if (handleAck(ack)) {
                    System.out.println("✓ Bail renouvelé avec succès!");
                } else {
                    System.out.println("✗ Erreur lors du renouvellement");
                }
            } else {
                System.out.println("✗ Erreur: réponse inattendue du serveur");
            }
            
        } catch (Exception e) {
            log("ERREUR lors du renouvellement: " + e.getMessage());
        }
        
        pauseAndContinue();
    }
    
    /**
     * Change l'adresse du serveur
     */
    private void changeServer() {
        System.out.println("\n--- Changement de serveur ---");
        System.out.println("Serveur actuel: " + serverAddress + ":" + serverPort);
        System.out.print("Nouvelle adresse IP du serveur: ");
        
        String newServer = scanner.nextLine().trim();
        if (!newServer.isEmpty()) {
            this.serverAddress = newServer;
            this.connected = false;
            System.out.println("✓ Serveur changé vers: " + serverAddress);
            System.out.println("  Testez la connexion avant d'utiliser le nouveau serveur");
        } else {
            System.out.println("Changement annulé");
        }
        
        pauseAndContinue();
    }
    
    /**
     * Affiche les informations du client
     */
    private void showClientInfo() {
        System.out.println("\n--- Informations du client ---");
        System.out.println("ID Client: " + getClientId());
        System.out.println("Serveur cible: " + serverAddress + ":" + serverPort);
        System.out.println("État connexion: " + (connected ? "Connecté" : "Non testé/Échec"));
        System.out.println("Configuration IP: " + (hasValidConfiguration() ? "Active" : "Aucune"));
        
        if (hasValidConfiguration()) {
            System.out.println("\nDétails configuration:");
            System.out.println("  IP assignée: " + getAssignedIP().getHostAddress());
        }
        
        pauseAndContinue();
    }
    
    /**
     * Vérifie la connexion avant d'effectuer une action
     */
    private boolean checkConnection() {
        if (!connected) {
            System.out.println("⚠ Connexion au serveur non vérifiée");
            System.out.print("Voulez-vous tester la connexion maintenant ? (o/n): ");
            String test = scanner.nextLine().trim().toLowerCase();
            
            if (test.equals("o") || test.equals("oui")) {
                if (testServerConnection()) {
                    connected = true;
                    System.out.println("✓ Connexion établie");
                    return true;
                } else {
                    System.out.println("✗ Impossible de se connecter au serveur");
                    return false;
                }
            } else {
                System.out.println("Action annulée");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Pause et attend que l'utilisateur appuie sur Entrée
     */
    private void pauseAndContinue() {
        System.out.print("\nAppuyez sur Entrée pour continuer...");
        scanner.nextLine();
    }
    
    // Méthodes existantes inchangées...
    
    /**
     * Envoie un message au serveur et reçoit la réponse
     */
    private DHCPMessage sendMessageToServer(DHCPMessage message) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            // Envoyer le message
            out.writeObject(message);
            out.flush();
            
            // Recevoir la réponse
            return (DHCPMessage) in.readObject();
            
        } catch (ConnectException e) {
            throw new IOException("Impossible de se connecter au serveur DHCP à " + serverAddress + ":" + serverPort, e);
        }
    }
    
    /**
     * Processus DHCP complet via le réseau
     */
    public boolean requestIPConfigurationNetwork() {
        try {
            log("Démarrage du processus DHCP avec le serveur " + serverAddress);
            
            // 1. Envoi DISCOVER
            DiscoverMessage discover = new DiscoverMessage(getClientId());
            log("Envoi DISCOVER...");
            
            DHCPMessage response1 = sendMessageToServer(discover);
            if (!(response1 instanceof OfferMessage)) {
                log("ERREUR: Réponse OFFER attendue, reçu: " + response1.getClass().getSimpleName());
                return false;
            }
            
            OfferMessage offer = (OfferMessage) response1;
            log("OFFER reçu: IP proposée " + offer.getOfferedIP().getHostAddress());
            
            // 2. Envoi REQUEST
            RequestMessage request = new RequestMessage(
                getClientId(),
                offer.getOfferedIP(),
                offer.getServerAddress()
            );
            log("Envoi REQUEST pour IP " + offer.getOfferedIP().getHostAddress());
            
            DHCPMessage response2 = sendMessageToServer(request);
            if (!(response2 instanceof AckMessage)) {
                log("ERREUR: Réponse ACK attendue, reçu: " + response2.getClass().getSimpleName());
                return false;
            }
            
            AckMessage ack = (AckMessage) response2;
            log("ACK reçu: Configuration IP validée");
            
            // 3. Configuration locale
            return handleAck(ack);
            
        } catch (IOException e) {
            log("ERREUR de connexion: " + e.getMessage());
            return false;
        } catch (ClassNotFoundException e) {
            log("ERREUR de sérialisation: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log("ERREUR générale: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Teste la connectivité avec le serveur
     */
    public boolean testServerConnection() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverAddress, serverPort), 5000);
            log("Connexion au serveur " + serverAddress + " réussie");
            return true;
        } catch (IOException e) {
            log("Impossible de se connecter au serveur " + serverAddress + ": " + e.getMessage());
            return false;
        }
    }
    
    private void log(String message) {
        String logMessage = "[" + LocalDateTime.now() + "] [" + getClientId() + "] " + message;
        System.out.println(logMessage);
    }
}