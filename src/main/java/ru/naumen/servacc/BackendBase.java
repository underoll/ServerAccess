package ru.naumen.servacc;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import ru.naumen.servacc.activechannel.ActiveChannelsRegistry;
import ru.naumen.servacc.activechannel.FTPActiveChannel;
import ru.naumen.servacc.activechannel.SSHActiveChannel;
import ru.naumen.servacc.activechannel.SSHLocalForwardActiveChannel;
import ru.naumen.servacc.activechannel.TerminalActiveChannel;
import ru.naumen.servacc.activechannel.i.IActiveChannel;
import ru.naumen.servacc.activechannel.i.IActiveChannelThrough;
import ru.naumen.servacc.activechannel.sockets.ServerSocketWrapper;
import ru.naumen.servacc.activechannel.visitors.CloseActiveChannelVisitor;
import ru.naumen.servacc.config2.Account;
import ru.naumen.servacc.config2.HTTPAccount;
import ru.naumen.servacc.config2.Path;
import ru.naumen.servacc.config2.SSHAccount;
import ru.naumen.servacc.config2.i.IConfig;
import ru.naumen.servacc.platform.Command;
import ru.naumen.servacc.platform.OS;
import ru.naumen.servacc.util.Util;

/**
 * @author aarkaev
 * @since 08.09.2016
 */
public abstract class BackendBase<C> implements Backend {

    private static final int SSH_DEFAULT_PORT = 22;
    protected final Command browser;
    protected final Command terminal;
    protected final Command ftpBrowser;

    protected final ExecutorService executor;
    protected final IConnectionsManager<C> connections;
    private SSHAccount globalThrough;
    private GlobalThroughView globalThroughView;
    private final ActiveChannelsRegistry acRegistry;
    protected final SSHKeyLoader keyLoader;

    public BackendBase(OS system, ExecutorService executorService, ActiveChannelsRegistry acRegistry, SSHKeyLoader keyLoader,
        IConnectionsManager<C> connectionsManager)
    {
        this.browser = system.getBrowser();
        this.ftpBrowser = system.getFTPBrowser();
        this.terminal = system.getTerminal();

        this.executor = executorService;
        connections = connectionsManager;
        this.acRegistry = acRegistry;
        this.keyLoader = keyLoader;
    }

    /**
     * Retrieve SSH2 connection described by account using through connection (if not null) as tunnel.
     * Simple function creating exactly 1 (one) SSH2 connection.
     * Do not use this function - it is only extension of another getSSH2Client.
     *
     * @param account
     * @param through
     * @return
     * @throws Exception
     */
    protected abstract C getSSH2Client(SSHAccount account, C through) throws Exception;

    @Override
    public void openHTTPAccount(HTTPAccount account) throws Exception
    {
        browser.open(buildUrl(account));
    }

    @Override
    public SSHAccount getThrough(Account account)
    {
        SSHAccount throughAccount = null;
        if (account.getThrough() != null)
        {
            throughAccount = (SSHAccount) account.getThrough();
        }
        else if (globalThrough != null)
        {
            throughAccount = globalThrough;
        }

        return throughAccount;
    }

    protected Socket openFTPBrowser(SSHAccount account) throws IOException
    {
        try (ServerSocketWrapper server = new ServerSocketWrapper(SocketUtils.createListener(SocketUtils.LOCALHOST), account, FTPActiveChannel.class, acRegistry))
        {
            server.getServerSocket().setSoTimeout(SocketUtils.COLD_TIMEOUT);
            ftpBrowser.connect(server.getServerSocket().getLocalPort(), Collections.<String, String>emptyMap());
            return server.accept();
        }
    }

    protected Socket openTerminal(SSHAccount account, String path) throws IOException
    {
        try (ServerSocketWrapper server = new ServerSocketWrapper(SocketUtils.createListener(SocketUtils.LOCALHOST), account, TerminalActiveChannel.class, acRegistry))
        {
            Map<String, String> params = new HashMap<>(account.getParams());
            params.put("name", path);
            server.getServerSocket().setSoTimeout(SocketUtils.WARM_TIMEOUT);
            terminal.connect(server.getServerSocket().getLocalPort(), params);
            // FIXME: collect children and kill it on (on?)
            return server.accept();
        }
    }

    protected String buildUrl(HTTPAccount account) throws Exception
    {
        URL url = new URL(account.getURL());
        // Construct URL
        StringBuilder targetURL = new StringBuilder();
        // protocol
        targetURL.append(url.getProtocol()).append("://");
        // user (authentication) info
        String userInfo;
        if (account.getLogin() != null)
        {
            String password = account.getPassword();
            password = password != null ? password : "";
            userInfo = account.getLogin() + ":" + password;
        }
        else
        {
            userInfo = url.getUserInfo();
        }
        if (!Util.isEmptyOrNull(userInfo))
        {
            targetURL.append(userInfo).append('@');
        }
        // host and port
        final String remoteHost = url.getHost();
        final int remotePort = url.getPort() >= 0 ? url.getPort() : 80;
        String targetHost;
        int targetPort;
        SSHAccount throughAccount = getThrough(account);
        if (throughAccount != null)
        {
            targetHost = SocketUtils.LOCALHOST;
            targetPort = SocketUtils.getFreePort();
            localPortForward(throughAccount, SocketUtils.LOCALHOST, targetPort, remoteHost, remotePort);
        }
        else
        {
            targetHost = remoteHost;
            targetPort = remotePort;
        }
        targetURL.append(targetHost).append(':').append(targetPort);
        // path info
        targetURL.append(url.getPath());
        // query string
        if (url.getQuery() != null)
        {
            targetURL.append('?').append(url.getQuery());
        }
        return targetURL.toString();
    }


    @Override
    public void cleanup()
    {
        connections.cleanup();
    }

    @Override
    public void setGlobalThrough(SSHAccount account)
    {
        globalThrough = account;
        connections.clearCache();
        acRegistry.hideAllChannels();
    }

    @Override
    public void setGlobalThroughView(GlobalThroughView view)
    {
        globalThroughView = view;
    }

    @Override
    public void selectNewGlobalThrough(String uniqueIdentity, IConfig config)
    {
        Path path = Path.find(config, uniqueIdentity);
        if (path.found())
        {
            globalThroughView.setGlobalThroughWidget(path.path());
            setGlobalThrough(path.account());
        }
        else
        {
            clearGlobalThrough();
        }
    }

    @Override
    public void refresh(IConfig newConfig)
    {
        String identity = "";
        if (globalThrough != null)
        {
            identity = globalThrough.getUniqueIdentity();
        }
        selectNewGlobalThrough(identity, newConfig);
    }

    @Override
    public void clearGlobalThrough()
    {
        globalThroughView.clearGlobalThroughWidget();
        setGlobalThrough(null);
    }

    protected boolean isConnected(SSHAccount account)
    {
        return connections.containsKey(account.getUniqueIdentity());
    }

    protected void removeConnection(SSHAccount account)
    {
        connections.remove(account.getUniqueIdentity());
    }

    protected void createSSHActiveChannel(SSHAccount account, int port, int portThrough)
    {
        List<String> path = account.getUniquePathReversed();

        if (!path.isEmpty())
        {
            path.remove(path.size() - 1);
        }

        IActiveChannelThrough parent = acRegistry.findChannelThrough(path);

        new SSHActiveChannel(parent, acRegistry, account, port, portThrough, connections).save();
    }

    protected void removeSSHActiveChannel(SSHAccount account)
    {
        IActiveChannel channel = acRegistry.findChannel(account.getUniquePathReversed());

        if (channel != null)
        {
            channel.accept(new CloseActiveChannelVisitor());
        }
    }

    protected void createSSHLocalForwardActiveChannel(SSHAccount account, int port)
    {
        IActiveChannel channel = acRegistry.findChannel(account.getUniquePathReversed());

        if (channel instanceof SSHActiveChannel)
        {
            new SSHLocalForwardActiveChannel((SSHActiveChannel)channel, acRegistry, port).save();
        }
    }

    /**
     * Retrieve SSH2 connection described by account (follow "through chain").
     * Complex function, creating 0...n SSH2 connection.
     *
     * @param account
     * @return
     * @throws Exception
     */
    protected C getSSH2Client(SSHAccount account) throws Exception
    {
        if (isConnected(account))
        {
            return getConnection(account);
        }
        // follow the 'through' chain
        List<SSHAccount> throughChain = new ArrayList<>();
        SSHAccount cur = getThrough(account);
        while (cur != null)
        {
            if (throughChain.contains(cur))
            {
                // circular reference
                break;
            }
            throughChain.add(cur);
            if (isConnected(cur))
            {
                // account is found in the cache, no need to go further
                break;
            }
            cur = getThrough(cur);
        }
        Collections.reverse(throughChain);
        C last = null;
        for (SSHAccount through : throughChain)
        {
            if (isConnected(through))
            {
                last = getConnection(through);
            }
            else
            {
                last = getSSH2Client(through, last);
                saveConnection(through, last);
            }
        }
        C client = getSSH2Client(account, last);
        saveConnection(account, client);
        return client;
    }

    protected void saveConnection(SSHAccount account, C client)
    {
        connections.put(account.getUniqueIdentity(), client);
    }

    protected C getConnection(SSHAccount account)
    {
        return connections.get(account.getUniqueIdentity());
    }
}
