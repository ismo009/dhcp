package dhcp.messages;

/**
 * Message DISCOVER - Émis par un client pour découvrir les serveurs DHCP disponibles
 */
public class DiscoverMessage extends DHCPMessage {
    
    public DiscoverMessage(String clientId) {
        super(clientId, MessageType.DISCOVER);
    }
    
    @Override
    public String toString() {
        return String.format("DISCOVER - Client %s recherche une configuration IP", clientId);
    }
}