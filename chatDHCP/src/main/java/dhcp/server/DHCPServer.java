package dhcp.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dhcp.messages.AckMessage;
import dhcp.messages.DiscoverMessage;
import dhcp.messages.OfferMessage;
import dhcp.messages.RequestMessage;

/**
 * Serveur DHCP principal
 */
public class DHCPServer {
    protected IPPool ipPool;
    private InetAddress serverAddress;
    private InetAddress subnetMask;
    private InetAddress defaultGateway;
    private InetAddress dnsServer;
    private int defaultLeaseDuration;
    private ScheduledExecutorService scheduler;
    private PrintWriter logWriter;
    
    public DHCPServer() {
        this.ipPool = new IPPool();
        this.scheduler = Executors.newScheduledThreadPool(2);
        initializeLogger();
    }
    
    /**
     * Initialise le serveur avec les paramètres de configuration
     */
    public void initialize(String configFile) throws IOException {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            config.load(fis);
        }
        
        // Chargement de la configuration
        String startIP = config.getProperty("dhcp.pool.start");
        String endIP = config.getProperty("dhcp.pool.end");
        this.defaultLeaseDuration = Integer.parseInt(config.getProperty("dhcp.lease.duration", "3600"));
        
        try {
            this.subnetMask = InetAddress.getByName(config.getProperty("dhcp.subnet.mask"));
            this.defaultGateway = InetAddress.getByName(config.getProperty("dhcp.default.gateway"));
            this.dnsServer = InetAddress.getByName(config.getProperty("dhcp.dns.server"));
            this.serverAddress = InetAddress.getLocalHost();
            
            // Initialisation du pool d'adresses
            ipPool.initializePool(startIP, endIP);
            
            log("Serveur DHCP initialisé avec succès");
            log("Pool d'adresses: " + startIP + " - " + endIP);
            log("Durée de bail par défaut: " + defaultLeaseDuration + " secondes");
            
        } catch (UnknownHostException e) {
            log("Erreur lors de l'initialisation: " + e.getMessage());
            throw new IOException("Configuration invalide", e);
        }
        
        // Démarrage du nettoyage automatique des baux expirés
        startLeaseCleanupTask();
    }
    
    /**
     * Traite un message DISCOVER du client
     */
    public OfferMessage handleDiscover(DiscoverMessage discover) {
        log("Réception DISCOVER du client: " + discover.getClientId());
        
        // Recherche d'une IP disponible
        InetAddress availableIP = ipPool.getAvailableIP(discover.getClientId());
        if (availableIP == null) {
            log("ERREUR: Aucune adresse IP disponible pour le client " + discover.getClientId());
            return null;
        }
        
        // Création du message OFFER
        OfferMessage offer = new OfferMessage(
            discover.getClientId(),
            availableIP,
            subnetMask,
            defaultGateway,
            dnsServer,
            defaultLeaseDuration
        );
        offer.setServerAddress(serverAddress);
        
        log("Envoi OFFER au client " + discover.getClientId() + " - IP proposée: " + availableIP.getHostAddress());
        return offer;
    }
    
    /**
     * Traite un message REQUEST du client
     */
    public AckMessage handleRequest(RequestMessage request) {
        log("Réception REQUEST du client: " + request.getClientId() + " pour IP: " + request.getRequestedIP().getHostAddress());
        
        // Vérification que le serveur correspond
        if (!serverAddress.equals(request.getServerAddress())) {
            log("REQUEST ignoré - serveur différent");
            return null;
        }
        
        // Attribution de l'adresse IP
        boolean assigned = ipPool.assignIP(request.getClientId(), request.getRequestedIP(), defaultLeaseDuration);
        if (!assigned) {
            log("ERREUR: Impossible d'attribuer l'IP " + request.getRequestedIP().getHostAddress() + " au client " + request.getClientId());
            return null;
        }
        
        // Création du message ACK
        AckMessage ack = new AckMessage(
            request.getClientId(),
            request.getRequestedIP(),
            subnetMask,
            defaultGateway,
            dnsServer,
            defaultLeaseDuration
        );
        ack.setServerAddress(serverAddress);
        
        log("Envoi ACK au client " + request.getClientId() + " - IP attribuée: " + request.getRequestedIP().getHostAddress());
        return ack;
    }
    
    /**
     * Affiche les adresses IP disponibles
     */
    public void showAvailableIPs() {
        System.out.println("\n=== ADRESSES IP DISPONIBLES ===");
        java.util.List<InetAddress> availableIPs = ipPool.getAvailableIPs();
        if (availableIPs.isEmpty()) {
            System.out.println("Aucune adresse IP disponible");
        } else {
            System.out.println("Nombre d'adresses disponibles: " + availableIPs.size());
            for (InetAddress ip : availableIPs) {
                System.out.println("  - " + ip.getHostAddress());
            }
        }
        System.out.println();
    }
    
    /**
     * Affiche les baux actifs
     */
    public void showActiveLeases() {
        System.out.println("\n=== BAUX ACTIFS ===");
        java.util.List<Lease> activeLeases = ipPool.getActiveLeases();
        if (activeLeases.isEmpty()) {
            System.out.println("Aucun bail actif");
        } else {
            System.out.println("Nombre de baux actifs: " + activeLeases.size());
            for (Lease lease : activeLeases) {
                System.out.println("  - " + lease.toString());
            }
        }
        System.out.println();
    }
    
    /**
     * Démarre la tâche de nettoyage automatique des baux expirés
     */
    private void startLeaseCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            log("Nettoyage automatique des baux expirés...");
            ipPool.cleanExpiredLeases();
        }, 60, 60, TimeUnit.SECONDS); // Vérification toutes les minutes
    }
    
    /**
     * Interface en ligne de commande pour le serveur
     */
    public void startCommandInterface() {
        Scanner scanner = new Scanner(System.in);
        String command;
        
        System.out.println("\n=== SERVEUR DHCP - INTERFACE DE COMMANDE ===");
        System.out.println("Commandes disponibles:");
        System.out.println("  'available' - Afficher les adresses IP disponibles");
        System.out.println("  'leases' - Afficher les baux actifs");
        System.out.println("  'clean' - Nettoyer les baux expirés");
        System.out.println("  'quit' - Arrêter le serveur");
        System.out.println("================================================\n");
        
        while (true) {
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
                case "quit":
                    shutdown();
                    return;
                default:
                    System.out.println("Commande inconnue. Tapez 'available', 'leases', 'clean' ou 'quit'.\n");
            }
        }
    }
    
    /**
     * Initialise le système de logs
     */
    private void initializeLogger() {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            FileWriter fw = new FileWriter("logs/server.log", true);
            logWriter = new PrintWriter(fw, true);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation des logs: " + e.getMessage());
        }
    }
    
    /**
     * Écrit un message dans les logs
     */
    private void log(String message) {
        String logMessage = "[" + LocalDateTime.now() + "] " + message;
        System.out.println(logMessage);
        if (logWriter != null) {
            logWriter.println(logMessage);
        }
    }
    
    /**
     * Arrête le serveur proprement
     */
    public void shutdown() {
        log("Arrêt du serveur DHCP...");
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (logWriter != null) {
            logWriter.close();
        }
        System.out.println("Serveur arrêté.");
    }
}