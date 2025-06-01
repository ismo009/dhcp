package dhcp.messages;

import java.net.InetAddress;

/**
 * Message OFFER - Émis par le serveur pour proposer une configuration IP
 */
public class OfferMessage extends DHCPMessage {
    private InetAddress offeredIP;
    private InetAddress subnetMask;
    private InetAddress defaultGateway;
    private InetAddress dnsServer;
    private int leaseDuration; // en secondes
    
    public OfferMessage(String clientId, InetAddress offeredIP, InetAddress subnetMask, 
                       InetAddress defaultGateway, InetAddress dnsServer, int leaseDuration) {
        super(clientId, MessageType.OFFER);
        this.offeredIP = offeredIP;
        this.subnetMask = subnetMask;
        this.defaultGateway = defaultGateway;
        this.dnsServer = dnsServer;
        this.leaseDuration = leaseDuration;
    }
    
    // Getters
    public InetAddress getOfferedIP() { return offeredIP; }
    public InetAddress getSubnetMask() { return subnetMask; }
    public InetAddress getDefaultGateway() { return defaultGateway; }
    public InetAddress getDnsServer() { return dnsServer; }
    public int getLeaseDuration() { return leaseDuration; }
    
    @Override
    public String toString() {
        return String.format("OFFER - IP proposée: %s, Masque: %s, Passerelle: %s, DNS: %s, Durée bail: %d sec", 
                           offeredIP.getHostAddress(), subnetMask.getHostAddress(), 
                           defaultGateway.getHostAddress(), dnsServer.getHostAddress(), leaseDuration);
    }
}