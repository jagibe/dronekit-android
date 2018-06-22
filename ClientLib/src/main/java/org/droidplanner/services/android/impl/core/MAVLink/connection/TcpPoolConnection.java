package org.droidplanner.services.android.impl.core.MAVLink.connection;

import android.content.Context;
import android.os.Bundle;

import org.droidplanner.services.android.impl.utils.NetworkUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import timber.log.Timber;

/**
 * Provides support for mavlink connection via TCP.
 */
public abstract class TcpPoolConnection extends MavLinkConnection {

    private static final int CONNECTION_TIMEOUT = 20 * 1000; // 20 secs in ms
    private Context c;
    private Socket socket;
    private BufferedOutputStream mavOut;
    private BufferedInputStream mavIn;

    private String serverIP;
    private int serverPort;
    private String imei;
    private String password;

    protected TcpPoolConnection(Context context) {
        super(context); c=context;
    }

    @Override
    public final void openConnection(Bundle connectionExtras) throws IOException {
        getTCPStream(connectionExtras);
        if(registerMe())
            onConnectionOpened(connectionExtras);
        else
            closeConnection();
    }

    @Override
    public final int readDataBlock(byte[] buffer) throws IOException {
        return mavIn.read(buffer);
    }

    @Override
    public final void sendBuffer(byte[] buffer) throws IOException {
        if (mavOut != null) {
            mavOut.write(buffer);
            mavOut.flush();
        }
    }

    @Override
    public final void loadPreferences() {
        serverIP = loadServerIP();
        serverPort = loadServerPort();
        imei = loadImei();
        password = loadPassword();
    }

    protected abstract int loadServerPort();
    protected abstract String loadServerIP();
    protected abstract String loadImei();
    protected abstract String loadPassword();

    @Override
    public final void closeConnection() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    private void getTCPStream(Bundle extras) throws IOException {
        InetAddress serverAddr = InetAddress.getByName(serverIP);
        socket = new Socket();
        NetworkUtils.bindSocketToNetwork(extras, socket);
        socket.connect(new InetSocketAddress(serverAddr, serverPort), CONNECTION_TIMEOUT);
        mavOut = new BufferedOutputStream((socket.getOutputStream()));
        mavIn = new BufferedInputStream(socket.getInputStream());
    }

    private boolean registerMe() throws IOException{
        char c = 30;
        mavOut.write(("cgs"+c+imei+c+password+"\n").getBytes());
        mavOut.flush();
        String str="";
        while(!str.startsWith(""+c)) {
            byte buff[] = new byte[20];
            mavIn.read(buff);
            str = new String(buff);
        }

        String parts[] = str.split(""+c);
        Timber.d("Recibido: " + Arrays.toString(parts));
        if(parts.length == 3) {
            if(parts[1].equals("0")) {
                return true;
            } else if(parts[1].equals("1")){
                return false;
            } else if(parts[1].equals("2")) {
                return false;
            }
        }
        System.out.println("ERROR");
        return false;
    }

    @Override
    public final int getConnectionType() {
        return MavLinkConnectionTypes.MAVLINK_CONNECTION_TCP_POOL;
    }
}
