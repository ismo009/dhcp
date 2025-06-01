package dhcp.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dhcp.messages.DHCPMessage;
import dhcp.messages.DiscoverMessage;
import dhcp.messages.OfferMessage;

/**
 * Gestionnaire de découverte DHCP avec support broadcast et multiples serveurs
 */
public class DHCPDiscoveryManager {
    private String clientId;
    private int discoveryPort = 6767;
    private int timeoutSeconds = 10;
    private List<String> knownServers;
    private ExecutorService executor;
    
    public DHCPDiscoveryManager(String clientId) {
        this.clientId = clientId;
        this.knownServers = new ArrayList<>();
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Ajoute un serveur connu à la liste
     */
    public void addKnownServer(String serverAddress) {
        if (!knownServers.contains(serverAddress)) {
            knownServers.add(serverAddress);
        }
    }
    
    /**
     * Découverte avec broadcast sur le réseau local
     */
    public List<OfferMessage> discoverWithBroadcast() {
        List<OfferMessage> offers = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        try {
            log("Démarrage de la découverte broadcast...");
            
            // Scanner le réseau local pour les serveurs DHCP
            List<String> networkAddresses = scanLocalNetwork();
            
            // Envoyer DISCOVER à tous les serveurs potentiels
            List<Future<OfferMessage>> futures = new ArrayList<>();
            
            for (String serverAddress : networkAddresses) {
                Future<OfferMessage> future = executor.submit(() -> {
                    try {
                        return sendDiscoverToServer(serverAddress);
                    } catch (Exception e) {
                        return null; // Serveur non disponible
                    }
                });
                futures.add(future);
            }
            
            // Collecter les offres avec timeout
            for (Future<OfferMessage> future : futures) {
                try {
                    OfferMessage offer = future.get(timeoutSeconds, TimeUnit.SECONDS);
                    if (offer != null) {
                        offers.add(offer);
                        log("Offre reçue de " + offer.getServerAddress().getHostAddress());
                    }
                } catch (TimeoutException e) {
                    future.cancel(true);
                } catch (Exception e) {
                    log("Erreur lors de la réception d'offre: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log("Erreur lors de la découverte broadcast: " + e.getMessage());
        }
        
        log("Découverte terminée. " + offers.size() + " offre(s) reçue(s)");
        return offers;
    }
    
    /**
     * Découverte sur les serveurs connus seulement
     */
    public List<OfferMessage> discoverKnownServers() {
        List<OfferMessage> offers = new ArrayList<>();
        
        if (knownServers.isEmpty()) {
            log("Aucun serveur connu configuré");
            return offers;
        }
        
        log("Découverte sur " + knownServers.size() + " serveur(s) connu(s)...");
        
        List<Future<OfferMessage>> futures = new ArrayList<>();
        
        for (String serverAddress : knownServers) {
            Future<OfferMessage> future = executor.submit(() -> {
                try {
                    return sendDiscoverToServer(serverAddress);
                } catch (Exception e) {
                    log("Serveur " + serverAddress + " non disponible: " + e.getMessage());
                    return null;
                }
            });
            futures.add(future);
        }
        
        // Collecter les offres
        for (Future<OfferMessage> future : futures) {
            try {
                OfferMessage offer = future.get(timeoutSeconds, TimeUnit.SECONDS);
                if (offer != null) {
                    offers.add(offer);
                    log("Offre reçue de " + offer.getServerAddress().getHostAddress());
                }
            } catch (Exception e) {
                log("Erreur lors de la réception d'offre: " + e.getMessage());
            }
        }
        
        log("Découverte terminée. " + offers.size() + " offre(s) reçue(s)");
        return offers;
    }
    
    /**
     * Envoie un DISCOVER à un serveur spécifique
     */
    private OfferMessage sendDiscoverToServer(String serverAddress) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverAddress, discoveryPort), 3000);
            
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
                DiscoverMessage discover = new DiscoverMessage(clientId);
                out.writeObject(discover);
                out.flush();
                
                DHCPMessage response = (DHCPMessage) in.readObject();
                
                if (response instanceof OfferMessage) {
                    return (OfferMessage) response;
                }
            }
        }
        return null;
    }
    
    /**
     * Scanner le réseau local pour trouver des serveurs DHCP potentiels
     */
    private List<String> scanLocalNetwork() {
        List<String> addresses = new ArrayList<>();
        
        try {
            // Obtenir l'adresse réseau locale
            InetAddress localHost = InetAddress.getLocalHost();
            String localIP = localHost.getHostAddress();
            
            log("IP locale détectée: " + localIP);
            
            // Extraire le préfixe réseau (suppose /24)
            String[] parts = localIP.split("\\.");
            if (parts.length == 4) {
                String networkPrefix = parts[0] + "." + parts[1] + "." + parts[2] + ".";
                
                log("Scan du réseau " + networkPrefix + "1-254");
                
                // Ajouter les serveurs connus d'abord
                addresses.addAll(knownServers);
                
                // Scanner quelques adresses communes ET la plage complète
                for (int i = 1; i <= 254; i++) {
                    String address = networkPrefix + i;
                    if (!addresses.contains(address)) {
                        addresses.add(address);
                    }
                }
            }
            
            // Ajouter l'adresse locale (pour les tests)
            if (!addresses.contains("localhost")) addresses.add("localhost");
            if (!addresses.contains("127.0.0.1")) addresses.add("127.0.0.1");
            
        } catch (Exception e) {
            log("Erreur lors du scan réseau: " + e.getMessage());
            // Fallback sur les adresses par défaut
            addresses.add("localhost");
            addresses.add("127.0.0.1");
            addresses.addAll(knownServers);
        }
        
        log("Adresses à scanner: " + addresses.size());
        return addresses;
    }
    
    /**
     * Sélectionne la meilleure offre selon différents critères
     */
    public OfferMessage selectBestOffer(List<OfferMessage> offers) {
        if (offers.isEmpty()) {
            log("Aucune offre à évaluer");
            return null;
        }
        
        if (offers.size() == 1) {
            log("Une seule offre disponible, sélection automatique");
            return offers.get(0);
        }
        
        log("Évaluation de " + offers.size() + " offres...");
        
        // Critères de sélection (dans l'ordre de priorité)
        OfferMessage bestOffer = offers.get(0);
        int bestScore = evaluateOffer(bestOffer);
        
        for (int i = 1; i < offers.size(); i++) {
            OfferMessage currentOffer = offers.get(i);
            int currentScore = evaluateOffer(currentOffer);
            
            if (currentScore > bestScore) {
                bestOffer = currentOffer;
                bestScore = currentScore;
            }
        }
        
        log("Meilleure offre sélectionnée: " + bestOffer.getServerAddress().getHostAddress() + 
            " (score: " + bestScore + ")");
        
        return bestOffer;
    }
    
    /**
     * Évalue une offre selon plusieurs critères et retourne un score
     */
    private int evaluateOffer(OfferMessage offer) {
        int score = 0;
        
        // Critère 1: Durée du bail (plus long = mieux)
        int leaseDuration = offer.getLeaseDuration();
        if (leaseDuration >= 7200) score += 30;      // 2h+
        else if (leaseDuration >= 3600) score += 20; // 1h+
        else if (leaseDuration >= 1800) score += 10; // 30min+
        
        // Critère 2: Serveur connu (priorité)
        String serverIP = offer.getServerAddress().getHostAddress();
        if (knownServers.contains(serverIP)) {
            score += 50;
        }
        
        // Critère 3: Adresse IP préférentielle (réseau local)
        try {
            InetAddress offeredIP = offer.getOfferedIP();
            String ipStr = offeredIP.getHostAddress();
            
            // Préférer les adresses dans certaines plages
            if (ipStr.startsWith("192.168.1.")) score += 20;
            else if (ipStr.startsWith("192.168.")) score += 15;
            else if (ipStr.startsWith("10.")) score += 10;
            
        } catch (Exception e) {
            // Ignore
        }
        
        // Critère 4: Serveurs DNS (Google DNS = bonus)
        try {
            String dnsIP = offer.getDnsServer().getHostAddress();
            if (dnsIP.equals("8.8.8.8") || dnsIP.equals("1.1.1.1")) {
                score += 15;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        // Critère 5: Temps de réponse (première offre reçue = bonus)
        // Ceci pourrait être implémenté en mesurant les timestamps
        
        return score;
    }
    
    /**
     * Affiche toutes les offres reçues pour comparaison
     */
    public void displayOffers(List<OfferMessage> offers) {
        if (offers.isEmpty()) {
            System.out.println("Aucune offre reçue");
            return;
        }
        
        System.out.println("\n=== OFFRES DHCP REÇUES ===");
        for (int i = 0; i < offers.size(); i++) {
            OfferMessage offer = offers.get(i);
            int score = evaluateOffer(offer);
            
            System.out.println("\nOffre " + (i + 1) + " (Score: " + score + "):");
            System.out.println("  Serveur: " + offer.getServerAddress().getHostAddress());
            System.out.println("  IP proposée: " + offer.getOfferedIP().getHostAddress());
            System.out.println("  Masque: " + offer.getSubnetMask().getHostAddress());
            System.out.println("  Passerelle: " + offer.getDefaultGateway().getHostAddress());
            System.out.println("  DNS: " + offer.getDnsServer().getHostAddress());
            System.out.println("  Durée bail: " + offer.getLeaseDuration() + " sec");
        }
        System.out.println("========================\n");
    }
    
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    private void log(String message) {
        System.out.println("[" + LocalDateTime.now() + "] [DISCOVERY] " + message);
    }
}