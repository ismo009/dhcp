package dhcp.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import dhcp.messages.AckMessage;
import dhcp.messages.DHCPMessage;
import dhcp.messages.OfferMessage;
import dhcp.messages.RequestMessage;

/**
 * Client DHCP avec support multi-serveurs et découverte broadcast
 */
public class DHCPClientNetworkMulti extends DHCPClient {
    private DHCPDiscoveryManager discoveryManager;
    private Scanner scanner;
    private List<String> knownServers;
    private OfferMessage selectedOffer;
    
    public DHCPClientNetworkMulti() {
        super();
        this.discoveryManager = new DHCPDiscoveryManager(getClientId());
        this.scanner = new Scanner(System.in);
        this.knownServers = new ArrayList<>();
    }
    
    public DHCPClientNetworkMulti(String clientId) {
        super(clientId);
        this.discoveryManager = new DHCPDiscoveryManager(clientId);
        this.scanner = new Scanner(System.in);
        this.knownServers = new ArrayList<>();
    }
    
    /**
     * Interface interactive avec support multi-serveurs
     */
    public void startMultiServerInterface() {
        System.out.println("\n=== CLIENT DHCP MULTI-SERVEURS ===");
        System.out.println("Client ID: " + getClientId());
        System.out.println("==================================");
        
        while (true) {
            showMultiServerMenu();
            int choice = getMenuChoice();
            
            switch (choice) {
                case 1:
                    manageKnownServers();
                    break;
                case 2:
                    discoverServersOnly();
                    break;
                case 3:
                    requestConfigurationWithChoice();
                    break;
                case 4:
                    requestConfigurationAutomatic();
                    break;
                case 5:
                    showLastOffers();
                    break;
                case 6:
                    showCurrentConfiguration();
                    break;
                case 7:
                    configureDiscoverySettings();
                    break;
                case 8:
                    System.out.println("Au revoir !");
                    discoveryManager.shutdown();
                    return;
                default:
                    System.out.println("Choix invalide. Veuillez réessayer.\n");
            }
        }
    }
    
    /**
     * Affiche le menu multi-serveurs
     */
    private void showMultiServerMenu() {
        System.out.println("\n=== MENU CLIENT DHCP MULTI-SERVEURS ===");
        System.out.println("1. Gérer les serveurs connus");
        System.out.println("2. Découvrir les serveurs disponibles");
        System.out.println("3. Demander config IP avec choix manuel d'offre");
        System.out.println("4. Demander config IP automatique (meilleure offre)");
        System.out.println("5. Afficher les dernières offres reçues");
        System.out.println("6. Afficher ma configuration actuelle");
        System.out.println("7. Configurer la découverte");
        System.out.println("8. Quitter");
        System.out.print("Votre choix (1-8): ");
    }
    
    /**
     * Gestion des serveurs connus
     */
    private void manageKnownServers() {
        while (true) {
            System.out.println("\n--- Gestion des serveurs connus ---");
            System.out.println("Serveurs actuels:");
            if (knownServers.isEmpty()) {
                System.out.println("  Aucun serveur configuré");
            } else {
                for (int i = 0; i < knownServers.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + knownServers.get(i));
                }
            }
            
            System.out.println("\n1. Ajouter un serveur");
            System.out.println("2. Supprimer un serveur");
            System.out.println("3. Retour au menu principal");
            System.out.print("Choix: ");
            
            int choice = getMenuChoice();
            switch (choice) {
                case 1:
                    addKnownServer();
                    break;
                case 2:
                    removeKnownServer();
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Choix invalide");
            }
        }
    }
    
    /**
     * Ajoute un serveur connu
     */
    private void addKnownServer() {
        System.out.print("Adresse IP du serveur à ajouter: ");
        String serverIP = scanner.nextLine().trim();
        
        if (!serverIP.isEmpty() && !knownServers.contains(serverIP)) {
            knownServers.add(serverIP);
            discoveryManager.addKnownServer(serverIP);
            System.out.println("✓ Serveur " + serverIP + " ajouté");
        } else if (knownServers.contains(serverIP)) {
            System.out.println("Ce serveur est déjà dans la liste");
        } else {
            System.out.println("Adresse invalide");
        }
    }
    
    /**
     * Supprime un serveur connu
     */
    private void removeKnownServer() {
        if (knownServers.isEmpty()) {
            System.out.println("Aucun serveur à supprimer");
            return;
        }
        
        System.out.print("Numéro du serveur à supprimer (1-" + knownServers.size() + "): ");
        int index = getMenuChoice() - 1;
        
        if (index >= 0 && index < knownServers.size()) {
            String removed = knownServers.remove(index);
            System.out.println("✓ Serveur " + removed + " supprimé");
        } else {
            System.out.println("Numéro invalide");
        }
    }
    
    /**
     * Découvre les serveurs sans demander de configuration
     */
    private void discoverServersOnly() {
        System.out.println("\n--- Découverte des serveurs ---");
        System.out.println("1. Découverte broadcast (réseau local)");
        System.out.println("2. Découverte sur serveurs connus uniquement");
        System.out.print("Choix: ");
        
        int choice = getMenuChoice();
        List<OfferMessage> offers = new ArrayList<>();
        
        switch (choice) {
            case 1:
                offers = discoveryManager.discoverWithBroadcast();
                break;
            case 2:
                offers = discoveryManager.discoverKnownServers();
                break;
            default:
                System.out.println("Choix invalide");
                return;
        }
        
        discoveryManager.displayOffers(offers);
        pauseAndContinue();
    }
    
    /**
     * Demande configuration avec choix manuel de l'offre
     */
    private void requestConfigurationWithChoice() {
        System.out.println("\n--- Configuration IP avec choix manuel ---");
        
        // Découverte
        List<OfferMessage> offers = performDiscovery();
        if (offers.isEmpty()) {
            System.out.println("Aucune offre reçue. Impossible de continuer.");
            pauseAndContinue();
            return;
        }
        
        // Affichage et choix
        discoveryManager.displayOffers(offers);
        
        System.out.print("Choisissez une offre (1-" + offers.size() + ") ou 0 pour annuler: ");
        int choice = getMenuChoice();
        
        if (choice == 0) {
            System.out.println("Configuration annulée");
            return;
        }
        
        if (choice < 1 || choice > offers.size()) {
            System.out.println("Choix invalide");
            return;
        }
        
        OfferMessage selectedOffer = offers.get(choice - 1);
        acceptOfferAndConfigure(selectedOffer);
        
        pauseAndContinue();
    }
    
    /**
     * Demande configuration automatique (meilleure offre)
     */
    private void requestConfigurationAutomatic() {
        System.out.println("\n--- Configuration IP automatique ---");
        
        // Découverte
        List<OfferMessage> offers = performDiscovery();
        if (offers.isEmpty()) {
            System.out.println("Aucune offre reçue. Impossible de continuer.");
            pauseAndContinue();
            return;
        }
        
        // Sélection automatique de la meilleure offre
        OfferMessage bestOffer = discoveryManager.selectBestOffer(offers);
        if (bestOffer != null) {
            System.out.println("Offre automatiquement sélectionnée:");
            System.out.println("  Serveur: " + bestOffer.getServerAddress().getHostAddress());
            System.out.println("  IP: " + bestOffer.getOfferedIP().getHostAddress());
            
            acceptOfferAndConfigure(bestOffer);
        }
        
        pauseAndContinue();
    }
    
    /**
     * Effectue la découverte selon le choix de l'utilisateur
     */
    private List<OfferMessage> performDiscovery() {
        System.out.println("Mode de découverte:");
        System.out.println("1. Broadcast (réseau local)");
        System.out.println("2. Serveurs connus uniquement");
        System.out.print("Choix: ");
        
        int choice = getMenuChoice();
        
        switch (choice) {
            case 1:
                return discoveryManager.discoverWithBroadcast();
            case 2:
                return discoveryManager.discoverKnownServers();
            default:
                System.out.println("Choix invalide, utilisation du broadcast");
                return discoveryManager.discoverWithBroadcast();
        }
    }
    
    /**
     * Accepte une offre et configure le client
     */
    private void acceptOfferAndConfigure(OfferMessage offer) {
        try {
            // Envoyer REQUEST
            RequestMessage request = new RequestMessage(
                getClientId(),
                offer.getOfferedIP(),
                offer.getServerAddress()
            );
            
            log("Envoi REQUEST au serveur " + offer.getServerAddress().getHostAddress());
            
            DHCPMessage response = sendMessageToServer(
                offer.getServerAddress().getHostAddress(),
                6767,
                request
            );
            
            if (response instanceof AckMessage) {
                AckMessage ack = (AckMessage) response;
                if (handleAck(ack)) {
                    System.out.println("✓ Configuration IP appliquée avec succès!");
                    showCurrentConfiguration();
                } else {
                    System.out.println("✗ Erreur lors de l'application de la configuration");
                }
            } else {
                System.out.println("✗ Réponse inattendue du serveur");
            }
            
        } catch (Exception e) {
            log("ERREUR lors de l'acceptation de l'offre: " + e.getMessage());
        }
    }
    
    /**
     * Affiche les dernières offres
     */
    private void showLastOffers() {
        // Cette fonctionnalité nécessiterait de stocker les dernières offres
        System.out.println("Fonctionnalité à implémenter: historique des offres");
        pauseAndContinue();
    }
    
    /**
     * Configure les paramètres de découverte
     */
    private void configureDiscoverySettings() {
        System.out.println("\n--- Configuration de la découverte ---");
        System.out.print("Timeout de découverte en secondes (actuel: 10): ");
        
        try {
            int timeout = Integer.parseInt(scanner.nextLine().trim());
            if (timeout > 0 && timeout <= 60) {
                discoveryManager.setTimeoutSeconds(timeout);
                System.out.println("✓ Timeout configuré à " + timeout + " secondes");
            } else {
                System.out.println("Timeout invalide (doit être entre 1 et 60)");
            }
        } catch (NumberFormatException e) {
            System.out.println("Valeur invalide");
        }
        
        pauseAndContinue();
    }
    
    // Méthodes utilitaires
    
    private DHCPMessage sendMessageToServer(String serverAddress, int port, DHCPMessage message) 
            throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(serverAddress, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            out.writeObject(message);
            out.flush();
            
            return (DHCPMessage) in.readObject();
        }
    }
    
    private void showCurrentConfiguration() {
        System.out.println("\n--- Configuration actuelle ---");
        showConfiguration();
    }
    
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
    
    private void pauseAndContinue() {
        System.out.print("\nAppuyez sur Entrée pour continuer...");
        scanner.nextLine();
    }
    
    private void log(String message) {
        String logMessage = "[" + LocalDateTime.now() + "] [" + getClientId() + "] " + message;
        System.out.println(logMessage);
    }
}