package dhcp.server;

import java.net.InetAddress;
import java.time.LocalDateTime;

/**
 * Représente un bail DHCP pour une adresse IP
 */
public class Lease {
    private String clientId;
    private InetAddress ipAddress;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration; // en secondes
    
    public Lease(String clientId, InetAddress ipAddress, int duration) {
        this.clientId = clientId;
        this.ipAddress = ipAddress;
        this.duration = duration;
        this.startTime = LocalDateTime.now();
        this.endTime = startTime.plusSeconds(duration);
    }
    
    /**
     * Vérifie si le bail a expiré
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }
    
    /**
     * Renouvelle le bail
     */
    public void renew() {
        this.startTime = LocalDateTime.now();
        this.endTime = startTime.plusSeconds(duration);
    }
    
    /**
     * Retourne le temps restant en secondes
     */
    public long getRemainingTimeSeconds() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            return 0;
        }
        return java.time.Duration.between(now, endTime).getSeconds();
    }
    
    // Getters
    public String getClientId() { return clientId; }
    public InetAddress getIpAddress() { return ipAddress; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public int getDuration() { return duration; }
    
    @Override
    public String toString() {
        return String.format("Bail [Client: %s, IP: %s, Début: %s, Fin: %s, Temps restant: %d sec]",
                           clientId, ipAddress.getHostAddress(), startTime, endTime, getRemainingTimeSeconds());
    }
}