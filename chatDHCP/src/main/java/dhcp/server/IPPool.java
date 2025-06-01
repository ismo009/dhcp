package dhcp.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le pool d'adresses IP disponibles et les baux actifs
 */
public class IPPool {
    private Queue<InetAddress> availableIPs;
    private Map<String, Lease> activeLeases; // clientId -> Lease
    private Map<InetAddress, String> ipToClient; // IP -> clientId
    
    public IPPool() {
        this.availableIPs = new LinkedList<>();
        this.activeLeases = new ConcurrentHashMap<>();
        this.ipToClient = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialise le pool avec une plage d'adresses IP
     */
    public void initializePool(String startIP, String endIP) throws UnknownHostException {
        InetAddress start = InetAddress.getByName(startIP);
        InetAddress end = InetAddress.getByName(endIP);
        
        long startLong = ipToLong(start);
        long endLong = ipToLong(end);
        
        for (long i = startLong; i <= endLong; i++) {
            InetAddress ip = longToIP(i);
            availableIPs.offer(ip);
        }
        
        System.out.println("Pool initialisé avec " + availableIPs.size() + " adresses IP");
    }
    
    /**
     * Obtient une adresse IP disponible pour un client
     */
    public synchronized InetAddress getAvailableIP(String clientId) {
        // Vérifier si le client a déjà un bail actif
        Lease existingLease = activeLeases.get(clientId);
        if (existingLease != null && !existingLease.isExpired()) {
            return existingLease.getIpAddress();
        }
        
        // Nettoyer les baux expirés
        cleanExpiredLeases();
        
        // Obtenir une nouvelle IP
        if (availableIPs.isEmpty()) {
            return null; // Aucune IP disponible
        }
        
        return availableIPs.poll();
    }
    
    /**
     * Attribue une adresse IP à un client avec un bail
     */
    public synchronized boolean assignIP(String clientId, InetAddress ip, int leaseDuration) {
        if (ipToClient.containsKey(ip)) {
            return false; // IP déjà attribuée
        }
        
        Lease lease = new Lease(clientId, ip, leaseDuration);
        activeLeases.put(clientId, lease);
        ipToClient.put(ip, clientId);
        
        System.out.println("IP " + ip.getHostAddress() + " attribuée au client " + clientId);
        return true;
    }
    
    /**
     * Libère une adresse IP
     */
    public synchronized void releaseIP(String clientId) {
        Lease lease = activeLeases.remove(clientId);
        if (lease != null) {
            ipToClient.remove(lease.getIpAddress());
            availableIPs.offer(lease.getIpAddress());
            System.out.println("IP " + lease.getIpAddress().getHostAddress() + " libérée du client " + clientId);
        }
    }
    
    /**
     * Nettoie les baux expirés
     */
    public synchronized void cleanExpiredLeases() {
        List<String> expiredClients = new ArrayList<>();
        
        for (Map.Entry<String, Lease> entry : activeLeases.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredClients.add(entry.getKey());
            }
        }
        
        for (String clientId : expiredClients) {
            releaseIP(clientId);
            System.out.println("Bail expiré pour le client " + clientId);
        }
    }
    
    /**
     * Retourne la liste des adresses IP disponibles
     */
    public List<InetAddress> getAvailableIPs() {
        return new ArrayList<>(availableIPs);
    }
    
    /**
     * Retourne la liste des baux actifs
     */
    public List<Lease> getActiveLeases() {
        cleanExpiredLeases();
        return new ArrayList<>(activeLeases.values());
    }
    
    /**
     * Utilitaires pour convertir IP en long et vice versa
     */
    private long ipToLong(InetAddress ip) {
        byte[] bytes = ip.getAddress();
        long result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result |= ((long) (bytes[i] & 0xFF)) << (8 * (3 - i));
        }
        return result;
    }
    
    private InetAddress longToIP(long ip) throws UnknownHostException {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((ip >> 24) & 0xFF);
        bytes[1] = (byte) ((ip >> 16) & 0xFF);
        bytes[2] = (byte) ((ip >> 8) & 0xFF);
        bytes[3] = (byte) (ip & 0xFF);
        return InetAddress.getByAddress(bytes);
    }
}