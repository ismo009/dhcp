package dhcp.messages;

import java.net.InetAddress;

/**
 * Message ACK - Émis par le serveur pour confirmer l'attribution d'une configuration IP
 */
public class AckMessage extends DHCPMessage {
    private InetAddress assignedIP;
    private InetAddress subnetMask;
    private InetAddress defaultGateway;
    private InetAddress dnsServer;
    private int leaseDuration;
    
    public AckMessage(String clientId, InetAddress assignedIP, InetAddress subnetMask,
                     InetAddress defaultGateway, InetAddress dnsServer, int leaseDuration) {
        super(clientId, MessageType.ACK);
        this.assignedIP = assignedIP;
        this.subnetMask = subnetMask;
        this.defaultGateway = defaultGateway;
        this.dnsServer = dnsServer;
        this.leaseDuration = leaseDuration;
    }
    
    // Getters
    public InetAddress getAssignedIP() { return assignedIP; }
    public InetAddress getSubnetMask() { return subnetMask; }
    public InetAddress getDefaultGateway() { return defaultGateway; }
    public InetAddress getDnsServer() { return dnsServer; }
    public int getLeaseDuration() { return leaseDuration; }
    
    @Override
    public String toString() {
        return String.format("ACK - IP attribuée: %s, Masque: %s, Passerelle: %s, DNS: %s, Durée bail: %d sec", 
                           assignedIP.getHostAddress(), subnetMask.getHostAddress(),
                           defaultGateway.getHostAddress(), dnsServer.getHostAddress(), leaseDuration);
    }
}