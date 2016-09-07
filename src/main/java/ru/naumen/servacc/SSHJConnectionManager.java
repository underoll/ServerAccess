package ru.naumen.servacc;

import java.util.List;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.transport.Transport;

/**
 * @author aarkaev
 * @since 08.09.2016
 */
public class SSHJConnectionManager extends ConnectionsManagerBase<SSHClient> {

    @Override
    protected void closeConnections(List<SSHClient> connections) {
        connections.stream()
            .map(SSHClient::getTransport)
            .filter(Transport::isRunning)
            .forEach(t -> t.disconnect(DisconnectReason.BY_APPLICATION));
    }
}
