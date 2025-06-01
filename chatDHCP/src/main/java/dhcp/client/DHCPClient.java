package dhcp.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

import dhcp.messages.AckMessage;
import dhcp.messages.DiscoverMessage;
import dhcp.messages.OfferMessage;
import dhcp.messages.RequestMessage;
import dhcp.server.DHCPServer;

/**
 * Client DHCP
 */
public class DHCPClient {
    private String clientId;
    private InetAddress assignedIP;
    private InetAddress subnetMask;
    private InetAddress defaultGateway;
    private InetAddress dnsServer;
    private int leaseDuration;
    private LocalDateTime leaseStartTime;
    private PrintWriter logWriter;
    
    public DHCPClient() {
        this.clientId = generateClientId();
        initializeLogger();
    }
    
    public DHCPClient(String clientId) {
        this.clientId = clientId;
        initializeLogger();
    }
    
    /**
     * Génère un identifiant unique pour le client
     */
    private String generateClientId() {
        return "CLIENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Initie le processus de découverte DHCP
     */
    public DiscoverMessage sendDiscover() {
        log("Envoi du message DISCOVER...");
        DiscoverMessage discover = new DiscoverMessage(clientId);
        log("DISCOVER envoyé: " + discover.toString());
        return discover;
    }
    
    /**
     * Traite une réponse OFFER du serveur
     */
    public RequestMessage handleOffer(OfferMessage offer) {
        if (!offer.getClientId().equals(this.clientId)) {
            log("OFFER ignoré - destiné à un autre client");
            return null;
        }
        
        log("Réception OFFER: " + offer.toString());
        
        // Accepter l'offre et envoyer REQUEST
        RequestMessage request = new RequestMessage(
            clientId,
            offer.getOfferedIP(),
            offer.getServerAddress()
        );
        
        log("Envoi REQUEST pour accepter l'offre: " + request.toString());
        return request;
    }
    
    /**
     * Traite une réponse ACK du serveur
     */
    public boolean handleAck(AckMessage ack) {
        if (!ack.getClientId().equals(this.clientId)) {
            log("ACK ignoré - destiné à un autre client");
            return false;
        }
        
        log("Réception ACK: " + ack.toString());
        
        // Configuration du client avec les paramètres reçus
        this.assignedIP = ack.getAssignedIP();
        this.subnetMask = ack.getSubnetMask();
        this.defaultGateway = ack.getDefaultGateway();
        this.dnsServer = ack.getDnsServer();
        this.leaseDuration = ack.getLeaseDuration();
        this.leaseStartTime = LocalDateTime.now();
        
        log("Configuration IP reçue et appliquée:");
        log("  - IP: " + assignedIP.getHostAddress());
        log("  - Masque: " + subnetMask.getHostAddress());
        log("  - Passerelle: " + defaultGateway.getHostAddress());
        log("  - DNS: " + dnsServer.getHostAddress());
        log("  - Durée bail: " + leaseDuration + " secondes");
        
        return true;
    }
    
    /**
     * Processus complet DHCP (DISCOVER -> OFFER -> REQUEST -> ACK)
     */
    public boolean requestIPConfiguration(DHCPServer server) {
        try {
            // 1. Envoi DISCOVER
            DiscoverMessage discover = sendDiscover();
            
            // 2. Réception OFFER
            OfferMessage offer = server.handleDiscover(discover);
            if (offer == null) {
                log("ERREUR: Aucune offre reçue du serveur");
                return false;
            }
            
            // 3. Envoi REQUEST
            RequestMessage request = handleOffer(offer);
            if (request == null) {
                log("ERREUR: Impossible de traiter l'offre");
                return false;
            }
            
            // 4. Réception ACK
            AckMessage ack = server.handleRequest(request);
            if (ack == null) {
                log("ERREUR: Aucun ACK reçu du serveur");
                return false;
            }
            
            // 5. Configuration finale
            return handleAck(ack);
            
        } catch (Exception e) {
            log("ERREUR lors du processus DHCP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Affiche la configuration actuelle du client
     */
    public void showConfiguration() {
        System.out.println("\n=== CONFIGURATION CLIENT " + clientId + " ===");
        if (assignedIP != null) {
            System.out.println("IP assignée: " + assignedIP.getHostAddress());
            System.out.println("Masque de sous-réseau: " + subnetMask.getHostAddress());
            System.out.println("Passerelle par défaut: " + defaultGateway.getHostAddress());
            System.out.println("Serveur DNS: " + dnsServer.getHostAddress());
            System.out.println("Durée du bail: " + leaseDuration + " secondes");
            System.out.println("Début du bail: " + leaseStartTime);
            
            // Calcul du temps restant
            if (leaseStartTime != null) {
                LocalDateTime expireTime = leaseStartTime.plusSeconds(leaseDuration);
                long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), expireTime).getSeconds();
                System.out.println("Temps restant: " + Math.max(0, remainingSeconds) + " secondes");
            }
        } else {
            System.out.println("Aucune configuration IP assignée");
        }
        System.out.println("=================================\n");
    }
    
    /**
     * Initialise le système de logs pour le client
     */
    private void initializeLogger() {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            FileWriter fw = new FileWriter("logs/client.log", true);
            logWriter = new PrintWriter(fw, true);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation des logs client: " + e.getMessage());
        }
    }
    
    /**
     * Écrit un message dans les logs
     */
    private void log(String message) {
        String logMessage = "[" + LocalDateTime.now() + "] [" + clientId + "] " + message;
        System.out.println(logMessage);
        if (logWriter != null) {
            logWriter.println(logMessage);
        }
    }
    
    // Getters
    public String getClientId() { return clientId; }
    public InetAddress getAssignedIP() { return assignedIP; }
    public boolean hasValidConfiguration() { return assignedIP != null; }
    
    /**
     * Ferme les ressources du client
     */
    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}