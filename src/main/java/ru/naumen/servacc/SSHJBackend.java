package ru.naumen.servacc;

import java.util.concurrent.ExecutorService;
import net.schmizz.sshj.SSHClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.naumen.servacc.activechannel.ActiveChannelsRegistry;
import ru.naumen.servacc.backend.DualChannel;
import ru.naumen.servacc.config2.Account;
import ru.naumen.servacc.config2.HTTPAccount;
import ru.naumen.servacc.config2.SSHAccount;
import ru.naumen.servacc.config2.i.IConfig;
import ru.naumen.servacc.platform.OS;

/**
 * @author aarkaev
 * @since 07.09.2016
 */
public class SSHJBackend extends BackendBase<SSHClient> {

    private static final int SSH_DEFAULT_PORT = 22;
    private static final Logger LOGGER = LoggerFactory.getLogger(SSHJBackend.class);

    public SSHJBackend(OS system, ExecutorService executorService, ActiveChannelsRegistry acRegistry, SSHKeyLoader keyLoader)
    {
        super(system, executorService, acRegistry, keyLoader, new SSHJConnectionManager());
    }

    @Override
    public void openSSHAccount(SSHAccount account, String path) throws Exception {

    }

    @Override
    public void openHTTPAccount(HTTPAccount account) throws Exception {

    }

    @Override
    public void localPortForward(SSHAccount account, String localHost, int localPort, String remoteHost, int remotePort) throws Exception {

    }

    @Override
    public void browseViaFTP(SSHAccount account) throws Exception {

    }

    @Override
    public DualChannel openProxyConnection(String host, int port, SSHAccount account) throws Exception {
        return null;
    }

    @Override
    public SSHAccount getThrough(Account account) {
        return null;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void setGlobalThrough(SSHAccount account) {

    }

    @Override
    public void setGlobalThroughView(GlobalThroughView view) {

    }

    @Override
    public void selectNewGlobalThrough(String uniqueIdentity, IConfig config) {

    }

    @Override
    public void refresh(IConfig newConfig) {

    }

    @Override
    public void clearGlobalThrough() {

    }
}
