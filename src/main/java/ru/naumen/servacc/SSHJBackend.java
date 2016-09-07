package ru.naumen.servacc;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
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
        SSHClient client;
        if (isConnected(account))
        {
            client = getSSH2Client(account);
            // this is to force timeout when reusing a cached connection
            // in order to detect if a connection is hung more quickly
            try
            {
                final SSHClient clientCopy = client;
                Future<Object> f = this.executor.submit(() ->
                {
                    openSSHAccount(account, clientCopy, path);
                    return null;
                });
                f.get(SocketUtils.WARM_TIMEOUT, TimeUnit.MILLISECONDS);
                return;
            }
            catch (TimeoutException e)
            {
                removeSSHActiveChannel(account);
                removeConnection(account);
                LOGGER.error("Connection is broken, retrying", e);
            }
        }
        // try with "cold" timeout
        client = getSSH2Client(account);
        openSSHAccount(account, client, path);
    }

    private void openSSHAccount(final SSHAccount account, final SSHClient ssh, final String path) throws Exception
    {
        final Session session = ssh.startSession();
        try {

            session.allocateDefaultPTY();

            final Shell shell = session.startShell();

            final Socket term = openTerminal(account, path);

            new StreamCopier(shell.getInputStream(), term.getOutputStream())
                .bufSize(shell.getLocalMaxPacketSize())
                .spawn("stdout");

            new StreamCopier(shell.getErrorStream(), System.err)
                .bufSize(shell.getLocalMaxPacketSize())
                .spawn("stderr");

            // Now make System.in act as stdin. To exit, hit Ctrl+D (since that results in an EOF on System.in)
            // This is kinda messy because java only allows console input after you hit return
            // But this is just an example... a GUI app could implement a proper PTY
            new StreamCopier(term.getInputStream(), shell.getOutputStream())
                .bufSize(shell.getRemoteMaxPacketSize())
                .copy();

        } finally {
            session.close();
        }
    }

    @Override
    protected SSHClient getSSH2Client(SSHAccount account, SSHClient through) throws Exception
    {
        String host = account.getHost();
        int port = account.getPort() >= 0 ? account.getPort() : SSH_DEFAULT_PORT;
//        if (through != null)
//        {
//            int localPort = SocketUtils.getFreePort();
//            //FIXME: localize newLocalForward usage in localPortForward
//            through.getConnection().newLocalForward(SocketUtils.LOCALHOST, localPort, host, port);
//            return createSSH2Client(SocketUtils.LOCALHOST, localPort, true, account);
//        }
        return createSSH2Client(host, port, false, account);
    }

    private SSHClient createSSH2Client(String host, Integer port, boolean through, final SSHAccount account) throws Exception
    {
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.connect(host, port);
        ssh.authPassword(account.getLogin(), account.getPassword());
        return ssh;
    }

    @Override
    public void openHTTPAccount(HTTPAccount account) throws Exception {

    }

    @Override
    public void localPortForward(SSHAccount account, String localHost, int localPort, String remoteHost, int remotePort) throws Exception {
        SSHClient ssh = new SSHClient();

        ssh.loadKnownHosts();

        ssh.connect(account.getHost(), account.getPort());
        try {

            ssh.authPublickey(System.getProperty("user.name"));

            final LocalPortForwarder.Parameters params
                = new LocalPortForwarder.Parameters(localHost, localPort, remoteHost, remotePort);
            final ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(params.getLocalHost(), params.getLocalPort()));
            try {
                ssh.newLocalPortForwarder(params, ss).listen();
            } finally {
                ss.close();
            }

        } finally {
            ssh.disconnect();
        }

        createSSHLocalForwardActiveChannel(account, localPort);
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
