/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package com.criptext.socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

//import javax.microedition.io.Connector;
//import javax.microedition.io.SocketConnection;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;

import com.criptext.comunication.MessageManager;

import android.util.Log;



/**
 * Sockets based implementation of the DarkStarClient interface.
 *
 * @author Karel Herink
 */
public class DarkStarSocketClient implements DarkStarClient {

    private String host;
    private int port;
    private DarkStarListener responseHandler;
    private InputReader reader;
    private OutputWriter writer;
    private Socket s;

    private byte[] reconnectKey;
    private boolean loggedIn;
    private Hashtable channels = new Hashtable();
    private Vector requests = new Vector();


    public DarkStarSocketClient(String host, int port, DarkStarListener responseHandler) {
        //Log.d("DarkStarSocketClient - DarkStarSocketClient", "host: "+host+"port: "+port+"responseHandler: "+responseHandler);
        this.responseHandler = responseHandler;
        this.host = host;
        this.port = port;
    }

    /**
     * @see DarkStarClient#connect()
     *
     * @throws java.io.IOException
     */
    public void connect() throws IOException {

        //s = (SocketConnection) Connector.open("socket://" + this.host + ":" + this.port+"?"+ConnectionType.getConnectionString(), Connector.READ_WRITE);
        try{
            s = new Socket(this.host,this.port);
            s.setReceiveBufferSize(2048*2048);
            s.setSendBufferSize(2048*2048);
            //Log.d("DarkStarSocketClient - connect", "creando socket s:"+ s+"host"+this.host+"port"+this.port);

        }
        catch(Exception e){
            System.out.println(e.toString());
        }

        DataInputStream in = new DataInputStream(s.getInputStream());
        //Log.d("DarkStarSocketClient - connect", "definiendo in: "+in);
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        //Log.d("DarkStarSocketClient - connect", "definiendo out: "+out);

        reader = new InputReader(this, in);
        //Log.d("DarkStarSocketClient - connect", "reader: "+reader);
        writer = new OutputWriter(this, out);
        //Log.d("DarkStarSocketClient - connect", "writer: "+writer);
        new Thread(reader).start();
        //Log.d("DarkStarSocketClient - connect", "Empezar hilo reader");
        new Thread(writer).start();
        //Log.d("DarkStarSocketClient - connect", "Empezar hilo writer");

        //give the threads some time to set-up (should be done in a better way)
        try {
            //Log.d("DarkStarSocketClient - connect", "Empieza suspension del hilo durante 1000 (s)");
            Thread.sleep(1000);
            //Log.d("DarkStarSocketClient - connect", "Terminaron los 1000 (s) de la suspension del hilo");
        } catch (InterruptedException ex) {
            //Log.d("DarkStarSocketClient - connect", "Exception: "+ex);
            ex.printStackTrace();
        }
    }

    /**
     * @see DarkStarClient#disconnect()
     *
     * @throws java.io.IOException
     */
    public void disconnect() throws IOException {
        System.out.println("User  disconnect, trying to connect..");
        reader.setDisconnected(true);
        writer.setDisconnected(true);
        this.loggedIn = false;
        if(!s.isInputShutdown())
            s.shutdownInput();
        if(!s.isOutputShutdown())
            s.shutdownOutput();
        if(!s.isClosed())
            s.close();
        responseHandler.disconnected(true, "no hay conexion");
    }

    private class InputReader implements Runnable {
        private DarkStarSocketClient main;
        private DataInputStream in;
        private boolean disconnected = false;

        public InputReader(DarkStarSocketClient main, DataInputStream in) {
            //Log.d("InputReader - InputReader", "main: "+main+"inputstream"+in);
            this.main = main;
            this.in = in;
        }

        public void run() {
            //Log.d("InputReader - run", "main: "+main+"inputstream"+in);
            while (!disconnected) {
                try {
                    //http://stackoverflow.com/questions/36161105/socket-java-cant-receive-too-much-data/
                    int length = in.readShort() & 0xFFFF;
                    byte[] msgBytes = new byte[length];
                    Log.e("InputReader - run", "len: "+length);
                    in.readFully(msgBytes);
                    MessageBuffer buf = new MessageBuffer(msgBytes);
                    Log.e("InputReader - run", "buf: "+buf);
                    handleApplicationMessage(buf);
                }
                catch (EOFException ex) {
                    Log.d("InputReader - run", "EOFException mandando a disconnect()");
                    responseHandler.disconnectedBySocket();
//                    try {
//                        main.disconnect();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    //post a dummy empty message to wake up the writer - it will realize that we;return disconnected
//                    MessageBuffer dummy = new MessageBuffer(new byte[0]);
//                    postRequest(dummy);
                    break;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }

        public void setDisconnected(boolean disconnected) {
            this.disconnected = disconnected;
        }

        public boolean isDisconnected() {
            return disconnected;
        }
    }

    private class OutputWriter implements Runnable {
        private DarkStarSocketClient main;
        private DataOutputStream out;
        private boolean disconnected = false;

        public OutputWriter(DarkStarSocketClient main, DataOutputStream out) {
            //Log.d("OutputWriter - OutputWriter", "main: "+main+"outputstream"+out);
            this.main = main;
            this.out = out;
        }

        public void run() {
            //Log.d("OutputWriter - run", "main: "+main+"outputstream"+out);
            //Log.d("OutputWriter - run", "disconnected: "+disconnected);
            while (!disconnected) {
                synchronized (requests) {
                    while (requests.size() == 0) {
                        try {
                            requests.wait();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    MessageBuffer request = (MessageBuffer) requests.elementAt(0);
                    //System.out.println("Dark - Sending lenght: " + request.limit());
                    //Log.d("OutputWriter - run", "request: "+request);
                    //Log.d("OutputWriter - run", "removido: "+request);
                    requests.removeElementAt(0);
                    try {
                        //request with empty buffer means we have disconnected
                        if (disconnected) {
                            break;
                        }
                        out.write(request.getBuffer());
                        //Log.d("OutputWriter - run", "buffer: "+request.toString());
                        out.flush();
                        //if (debug) System.out.println("Wrote request");
                    } catch (IOException ex) {
                        if (!disconnected) {
                            ex.printStackTrace();
                            try {
                                Log.d("OutputWriter - run", "somethin happen calling disconnect()");
                                main.disconnect();
                            } catch (IOException ex1) {
                                //ignore
                                ex1.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        public void setDisconnected(boolean disconnected) {
            this.disconnected = disconnected;
        }

        public boolean isDisconnected() {
            return disconnected;
        }
    }

    private void handleApplicationMessage(MessageBuffer msg) throws Exception {
        byte command = msg.getByte();
        //Log.d("InputReader - handleApplicationMessage", "command: "+command);
        System.out.println("Dark - handleApplicationMessage command: "+command);

        switch (command) {
            case SimpleSgsProtocol.LOGIN_SUCCESS:
                reconnectKey = msg.getBytes(msg.limit() - msg.position());
                loggedIn = true;
                responseHandler.loggedIn();
                break;

            case SimpleSgsProtocol.LOGIN_FAILURE: {
                String reason = msg.getString();
                System.out.println("Login failed: " + reason);
                responseHandler.loginFailed(reason);
                break;
            }

            case SimpleSgsProtocol.LOGIN_REDIRECT: {
                String host = msg.getString();
                int port = msg.getInt();

                break;
            }

            case SimpleSgsProtocol.SESSION_MESSAGE: {
                checkLoggedIn();
                //System.out.println("Dark - limit:"+msg.limit()+" - position:"+msg.position());
                byte[] msgBytes = msg.getBytes(msg.limit() - msg.position());
                responseHandler.receivedSessionMessage(msgBytes);
                break;
            }

            case SimpleSgsProtocol.RECONNECT_SUCCESS:
                loggedIn = true;
                reconnectKey = msg.getBytes(msg.limit() - msg.position());
                responseHandler.reconnected();
                break;

            case SimpleSgsProtocol.RECONNECT_FAILURE:
                String reason = msg.getString();
                this.disconnect();
                break;

            case SimpleSgsProtocol.LOGOUT_SUCCESS:
                loggedIn = false;
                break;

            case SimpleSgsProtocol.CHANNEL_JOIN: {
                checkLoggedIn();
                String channelName = msg.getString();
                byte[] channelId = msg.getBytes(msg.limit() - msg.position());

                if (channels.get(channelName) == null) {
                    channels.put(channelName, channelId);
                }
                else {

                }
                responseHandler.joinedChannel(channelName);
                break;
            }

            case SimpleSgsProtocol.CHANNEL_LEAVE: {
                checkLoggedIn();
                byte[] channelId = msg.getBytes(msg.limit() - msg.position());
                String channelName = getChannelNameById(channelId);

                if (channelName != null) {
                    channels.remove(channelName);
                    responseHandler.leftChannel(channelName);
                } else {

                }
                break;
            }

            case SimpleSgsProtocol.CHANNEL_MESSAGE:
                checkLoggedIn();
                byte[] channelId = msg.getBytes(msg.getShort());
                String channelName = getChannelNameById(channelId);
                if (channelName == null) {
                    return;
                }
                byte[] msgBytes = msg.getBytes(msg.limit() - msg.position());
                responseHandler.receivedChannelMessage(channelName, msgBytes);
                break;

            default:
                throw new IOException("Unknown session opcode: " + command);
        }
    }

    // -------------------------------------------------------------------------
    // DarkStarClient methods
    // -------------------------------------------------------------------------

    //methods dealing with server session

    /**
     * @see DarkStarClient#login(java.lang.String, java.lang.String)
     *
     * @param userName
     * @param password
     * @throws java.io.IOException
     */
    public void login(String userName, String password) throws IOException {
        Log.d("DarkStarSocketClient", "userName: "+userName+"password: "+password);
        int len = 2 + MessageBuffer.getSize(userName) + MessageBuffer.getSize(password);
        Log.d("DarkStarSocketClient", "len: "+len);
        MessageBuffer msg = new MessageBuffer(2 + len);
        //Log.d("DarkStarSocketClient - login", "msg: "+msg);
        msg.putShort(len).
                putByte(SimpleSgsProtocol.LOGIN_REQUEST).
                putByte(SimpleSgsProtocol.VERSION).
                putString(userName).
                putString(password);
        postRequest(msg);
    }

    /**
     * @see DarkStarClient#isConnected()
     *
     * @return
     * @throws java.io.IOException
     */
    public boolean isConnected() throws IOException {
        return loggedIn;
    }

    /**
     * @see DarkStarClient#logout(boolean)
     * @param force
     * @throws java.io.IOException
     */
    public void  logout(boolean force) throws IOException {
        int len = 1;
        MessageBuffer msg = new MessageBuffer(2 + len);
        msg.putShort(len).
                putByte(SimpleSgsProtocol.LOGOUT_REQUEST);
        postRequest(msg);
    }

    /**
     * @see DarkStarClient#sendToSession(byte[])
     *
     * @param message
     * @throws java.io.IOException
     */
    public void  sendToSession(byte[] message) throws IOException {
        //Log.d("DarkStarSocketClient - sendToSession", "message: "+message);
        int len = 1 + message.length;
        //Log.d("DarkStarSocketClient - sendToSession", "len: "+len);
        MessageBuffer msg = new MessageBuffer(2 + len);
        //Log.d("DarkStarSocketClient - sendToSession", "msg: "+msg);
        msg.putShort(len).
                putByte(SimpleSgsProtocol.SESSION_MESSAGE).
                putBytes(message);
        //Log.d("DarkStarSocketClient - sendToSession", "otra vez msg: "+msg);
        postRequest(msg);
    }

    //methods dealing with client channels

    /**
     * @see DarkStarClient#sendToChannel(java.lang.String, byte[])
     *
     * @param channelName
     * @param message
     * @throws java.io.IOException
     */
    public void sendToChannel(String channelName, byte[] message) throws IOException {
        byte[] channelId = (byte[]) channels.get(channelName);
        int len = 1 + 2 + channelId.length + message.length;
        MessageBuffer msg = new MessageBuffer(2 + len);
        msg.putShort(len).
                putByte(SimpleSgsProtocol.CHANNEL_MESSAGE).
                putByteArray(channelId).
                putBytes(message);
        postRequest(msg);
    }


    //utility methods

    private void checkLoggedIn() {
        if (!loggedIn) {
            throw new IllegalStateException("Client not logged in");
        }
    }

    private void postRequest(MessageBuffer msg) {
        //Log.d("DarkStarSocketClient - postRequest", "msg: "+msg);
        synchronized(requests) {
            //Log.d("DarkStarSocketClient - postRequest", "requests: "+requests);
            requests.addElement(msg);
            //Log.d("DarkStarSocketClient - postRequest", "requests: "+requests);
            requests.notifyAll();
        }
    }

    private String getChannelNameById(byte[] channelId) {
        Enumeration keys = channels.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            byte[] id = (byte[]) channels.get(name);
            if (byteArraysEqual(channelId, id)) {
                return name;
            }
            continue;
        }
        return null;
    }

    private boolean byteArraysEqual(byte[] a, byte[] b) {
        if ((a == null && b != null) || (a != null && b == null)) {
            return false;
        }
        if (a == null && b == null) {
            return true;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}

