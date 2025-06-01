package dhcp.messages;

import java.net.InetAddress;

/**
 * Message REQUEST - Émis par le client pour accepter une offre du serveur
 */
public class RequestMessage extends DHCPMessage {
    private InetAddress requestedIP;
    
    public RequestMessage(String clientId, InetAddress requestedIP, InetAddress serverAddress) {
        super(clientId, MessageType.REQUEST);
        this.requestedIP = requestedIP;
        this.serverAddress = serverAddress;
    }
    
    public InetAddress getRequestedIP() { return requestedIP; }
    
    @Override
    public String toString() {
        return String.format("REQUEST - Client %s demande l'IP: %s au serveur: %s", 
                           clientId, requestedIP.getHostAddress(), 
                           serverAddress != null ? serverAddress.getHostAddress() : "inconnu");
    }
}