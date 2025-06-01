package dhcp.messages;

import java.io.Serializable;
import java.net.InetAddress;
import java.time.LocalDateTime;

/**
 * Classe de base pour tous les messages DHCP - maintenant sérialisable
 */
public abstract class DHCPMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected String clientId;
    protected InetAddress serverAddress;
    protected LocalDateTime timestamp;
    protected MessageType messageType;
    
    public enum MessageType {
        DISCOVER, OFFER, REQUEST, ACK
    }
    
    public DHCPMessage(String clientId, MessageType messageType) {
        this.clientId = clientId;
        this.messageType = messageType;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters et setters
    public String getClientId() { return clientId; }
    public MessageType getMessageType() { return messageType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public InetAddress getServerAddress() { return serverAddress; }
    public void setServerAddress(InetAddress serverAddress) { this.serverAddress = serverAddress; }
    
    @Override
    public String toString() {
        return String.format("[%s] Client: %s, Type: %s, Heure: %s", 
                           getClass().getSimpleName(), clientId, messageType, timestamp);
    }
}