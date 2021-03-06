/*
 * NetworkThread.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import com.galactanet.gametable.Log;



/**
 * Multiplexed network thread for all the network stuff.
 * 
 * @author iffy
 */
public class NetworkThread extends Thread
{
    /**
     * Private command class to set the interest ops between selections.
     * 
     * @author iffy
     */
    private class MarkForWriting implements Runnable
    {
        private final Connection connection;

        public MarkForWriting(final Connection c)
        {
            connection = c;
        }

        public void run()
        {
            try
            {
                connection.getKey().interestOps(connection.getKey().interestOps() | SelectionKey.OP_WRITE);
            }
            catch (final Throwable t)
            {
                Log.log(Log.NET, t);
            }
        }
    }

    /**
     * Private command class to set the interest ops between selections.
     * 
     * @author iffy
     */
    private class RegisterConnection implements Runnable
    {
        private final Connection connection;
        private final int        interestOps;

        public RegisterConnection(final Connection c, final int ops)
        {
            connection = c;
            interestOps = ops;
        }

        public void run()
        {
            try
            {
                connection.register(NetworkThread.this, interestOps);
            }
            catch (final Throwable t)
            {
                Log.log(Log.NET, t);
            }
        }
    }

    private final Set<Connection> connections     = new HashSet<Connection>();
    private final Set<Connection> lostConnections = new HashSet<Connection>();
    private final List<Runnable>  pendingCommands = new LinkedList<Runnable>();

    private Selector            selector;
    private final int           serverPort;
    private ServerSocketChannel serverSocketChannel;
    private boolean             startServer     = false;

    /**
     * Client Constructor.
     */
    public NetworkThread()
    {
        super(NetworkThread.class.getName());
        setPriority(NORM_PRIORITY + 1);
        serverPort = -1;
        startServer = false;
    }

    /**
     * Server Constructor.
     */
    public NetworkThread(final int port)
    {
        super(NetworkThread.class.getName());
        setPriority(NORM_PRIORITY + 1);
        serverPort = port;
        startServer = true;
    }

    public void add(final Connection connection)
    {
        boolean connected = false;
        try
        {
            connected = connection.getChannel().finishConnect();
        }
        catch (final IOException ioe)
        {
            Log.log(Log.NET, ioe);
        }

        if (connected)
        {
            connection.markConnected();
            connection.markLoggedIn();
            add(connection, SelectionKey.OP_READ);
        }
        else
        {
            add(connection, SelectionKey.OP_CONNECT);
        }
    }

    void add(final Connection connection, final int ops)
    {
        synchronized (connections)
        {
            connections.add(connection);
            synchronized (pendingCommands)
            {
                pendingCommands.add(new RegisterConnection(connection, ops));
            }
        }

        if (selector != null)
        {
            selector.wakeup();
        }
    }

    public void closeAllConnections()
    {
        try
        {
            synchronized (connections)
            {
                final Iterator<Connection> iter = connections.iterator();
                while (iter.hasNext())
                {
                    final Connection connection = iter.next();
                    connection.close();
                }
                connections.clear();
            }

            if (selector != null)
            {
                selector.close();
                selector = null;
            }

            if (serverSocketChannel != null)
            {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
        }
        catch (final Throwable t)
        {
            Log.log(Log.NET, t);
        }
    }

    private void cullLostConnections()
    {
        final Set<Connection> lost = new HashSet<Connection>();
        synchronized (connections)
        {
            final Iterator<Connection> iterator = connections.iterator();
            while (iterator.hasNext())
            {
                final Connection connection = iterator.next();
                if (connection.isDead())
                {
                    lost.add(connection);
                }
            }
        }

        final Iterator<Connection> iterator = lost.iterator();
        while (iterator.hasNext())
        {
            remove(iterator.next());
        }
    }

    public Set<Connection> getConnections()
    {
        final Set<Connection> retVal = new HashSet<Connection>();
        synchronized (connections)
        {
            retVal.addAll(connections);
            connections.clear();
        }

        return retVal;
    }

    public Set<Connection> getLostConnections()
    {
        cullLostConnections();

        Set<Connection> retVal = null;
        synchronized (lostConnections)
        {
            retVal = new HashSet<Connection>(lostConnections);
            lostConnections.clear();
            return retVal;
        }
    }

    public List<Packet> getPackets()
    {
        final List<Packet> retVal = new ArrayList<Packet>();
        synchronized (connections)
        {
            final Iterator<Connection> iter = connections.iterator();
            while (iter.hasNext())
            {
                final Connection connection = iter.next();
                while (connection.hasPackets())
                {
                    final byte[] data = connection.receivePacket();
                    if (data == null)
                    {
                        break;
                    }
                    retVal.add(new Packet(data, connection));
                }
            }
        }
        return retVal;
    }

    /**
     * @return Returns this thread's selector.
     */
    public Selector getSelector()
    {
        return selector;
    }

    public void markForWriting(final Connection c)
    {
        synchronized (pendingCommands)
        {
            pendingCommands.add(new MarkForWriting(c));
        }

        selector.wakeup();
    }

    public void remove(final Connection connection)
    {
        connection.close();
        synchronized (connections)
        {
            connections.remove(connection);
            synchronized (lostConnections)
            {
                lostConnections.add(connection);
            }
        }
    }

    /*
     * @see java.lang.Thread#run()
     */
    public void run()
    {
        try
        {
            selector = Selector.open();

            while (true)
            {
                sleep(1);

                if (startServer)
                {
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.socket().bind(new InetSocketAddress(serverPort));
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, this);
                    startServer = false;
                }

                synchronized (pendingCommands)
                {
                    while (pendingCommands.size() > 0)
                    {
                        final Runnable r = (Runnable)pendingCommands.remove(0);

                        try
                        {
                            r.run();
                        }
                        catch (final Exception e)
                        {
                            Log.log(Log.NET, e);
                        }
                    }
                }

                if (selector.selectNow() == 0)
                {
                    continue;
                }

                sleep(1);

                final Set keys = selector.selectedKeys();
                final Iterator keyIterator = keys.iterator();
                while (keyIterator.hasNext())
                {
                    try
                    {
                        final SelectionKey key = (SelectionKey)keyIterator.next();

                        if (key.isAcceptable())
                        {
                            final ServerSocketChannel keyChannel = (ServerSocketChannel)key.channel();
                            final SocketChannel newChannel = keyChannel.accept();
                            final Connection connection = new Connection(newChannel);
                            add(connection, SelectionKey.OP_READ);
                        }

                        if (key.isConnectable())
                        {
                            final SocketChannel keyChannel = (SocketChannel)key.channel();
                            final Connection connection = (Connection)key.attachment();
                            try
                            {
                                while (!keyChannel.finishConnect())
                                {
                                    // keep going
                                }
                                key.interestOps(SelectionKey.OP_READ);
                                connection.markConnected();
                                connection.markLoggedIn();
                            }
                            catch (final IOException ioe)
                            {
                                Log.log(Log.NET, ioe);
                                keyChannel.close();
                                connection.close();
                            }
                        }

                        if (key.isReadable())
                        {
                            final Connection connection = (Connection)key.attachment();
                            try
                            {
                                connection.readFromNet();
                            }
                            catch (final IOException ioe)
                            {
                                Log.log(Log.NET, ioe);
                                connection.close();
                            }
                        }

                        if (key.isWritable())
                        {
                            final Connection connection = (Connection)key.attachment();
                            try
                            {
                                connection.writeToNet();
                            }
                            catch (final IOException ioe)
                            {
                                Log.log(Log.NET, ioe);
                                connection.close();
                            }
                        }
                    }
                    catch (final CancelledKeyException cke)
                    {
                        Log.log(Log.NET, cke);
                    }
                    finally
                    {
                        keyIterator.remove();
                    }
                }
            }
        }
        catch (final InterruptedException ie)
        {
            Log.log(Log.SYS, ie);
        }
        catch (final Throwable t)
        {
            Log.log(Log.SYS, t);
        }
        finally
        {
            closeAllConnections();
        }
    }

    public void send(final byte[] packet)
    {
        synchronized (connections)
        {
            final Iterator iter = connections.iterator();
            while (iter.hasNext())
            {
                final Connection connection = (Connection)iter.next();
                if (connection.isLoggedIn())
                {
                    connection.sendPacket(packet);
                }
            }
        }
    }

    public void send(final byte[] packet, final Connection connection)
    {
        connection.sendPacket(packet);
    }
}
