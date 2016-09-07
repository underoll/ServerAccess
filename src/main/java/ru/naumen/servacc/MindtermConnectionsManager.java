package ru.naumen.servacc;

import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2Transport;
import java.util.List;

/**
 * Stores list of connections.
 *
 * @author tosha
 *         Extracted @since 22.11.12
 */
public class MindtermConnectionsManager extends ConnectionsManagerBase<SSH2SimpleClient> {

    @Override
    protected void closeConnections(List<SSH2SimpleClient> connections) {
        connections.stream()
            .map(SSH2SimpleClient::getTransport)
            .filter(SSH2Transport::isConnected)
            .forEach(t -> t.normalDisconnect("quit"));
    }
}
