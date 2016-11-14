/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * Nov 14, 2016
 */
package pt.lsts.neptus.plugins.uavparameters.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_param_value;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.uavparameters.MAVLinkParameters;

/**
 * @author Manuel R.
 *
 */
public class MAVLinkConnection {

    private static final int READ_BUFFER_SIZE = 1024;
    private final LinkedBlockingQueue<byte[]> mPacketsToSend = new LinkedBlockingQueue<>();
    private boolean toInitiateConnection = false;
    private boolean isMAVLinkConnected = false;
    private Socket tcpSocket = null;
    private String tcpHost;
    private int tcpPort;
    private BufferedOutputStream writer = null;
    private final ConcurrentHashMap<String, MAVLinkConnectionListener> mListeners = new ConcurrentHashMap<String, MAVLinkConnectionListener>();
    

    public MAVLinkConnection(String address, int port) {
        this.tcpHost = address;
        this.tcpPort = port;
    }

    public void connect() {
        final Socket socket = new Socket();
        final byte[] readBuffer = new byte[READ_BUFFER_SIZE];
        this.tcpSocket = socket;

        Thread listenerTask = new Thread("MAVLink TCP Listener") {
            @Override
            public void run() {

                BufferedInputStream reader = null;
                try {
                    socket.setSoTimeout(2000);
                }
                catch (SocketException e) {
                    e.printStackTrace();
                }
                try {

                    socket.connect(new InetSocketAddress(tcpHost, tcpPort));
                    reader = new BufferedInputStream(socket.getInputStream());
                    writer = new BufferedOutputStream(socket.getOutputStream());

                    setMAVLinkConnected(true);

                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error connecting via TCP to " + tcpHost + ":" + tcpPort + "\n" + e.getMessage());

                    setMAVLinkConnected(false);

                    reconnect(socket);

                    return;
                }
                NeptusLog.pub().info("Listening to MAVLink messages over TCP.");
                while (toInitiateConnection && isMAVLinkConnected) {
                    try {
                        MAVLinkPacket packet;
                        Parser parser = new Parser();

                        reader.read(readBuffer);

                        for (byte c : readBuffer) {
                            packet = parser.mavlink_parse_char(c & 0xFF);
                            if(packet != null){
                                //System.out.println(packet.msgid + " " + packet.sysid);
                                MAVLinkMessage msg = (MAVLinkMessage) packet.unpack();
                                
                                //System.out.println("MSG "+ msg.toString());
                                if (msg != null) {
                                    for (MAVLinkConnectionListener l : mListeners.values())
                                        l.onReceiveMessage(msg);
                                    
                                    if (msg.msgid == msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE) {
                                       // System.out.println(msg.toString());
                                        msg_param_value param = (msg_param_value) msg;
                                        System.out.println(param.toString());
                                    }
                               
                                }
                            }  
                        }
                    }
                    catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        continue;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                NeptusLog.pub().info("MAVLink TCP Socket closed.");
                try {
                    socket.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    setMAVLinkConnected(false);
                }

                reconnect(socket);

            }

            private void reconnect(final Socket socket) {
                if (toInitiateConnection) {
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (toInitiateConnection && !isMAVLinkConnected) {
                        connect();
                    }
                }
            };
        };
        listenerTask.setDaemon(true);
        setMAVLinkConnected(true);
        listenerTask.start();
    }

    public void send() {
        Thread writerTask = new Thread("MAVLink TCP Writer") {
            public void run() {

                try {
                    while (toInitiateConnection && isMAVLinkConnected) {
                        System.out.println("Waiting for data to be sent...");
                        byte[] buffer = mPacketsToSend.take();

                        if (writer != null) {
                            writer.write(buffer);
                            writer.flush();
                        }

                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };
        writerTask.setDaemon(true);
        writerTask.start();
    }

    public void closeConnection() throws IOException {
        if (tcpSocket != null) {
            tcpSocket.close();
            tcpSocket = null;
            toInitiateConnection = false;
            setMAVLinkConnected(false);
            System.out.println("Disconnected!");
        }
    }
    
    public void addMavLinkConnectionListener(String tag, MAVLinkConnectionListener listener) {
        mListeners.put(tag, listener);
    }

    public void removeMavLinkConnectionListener(String tag) {
        mListeners.remove(tag);
    }
    
    public void sendMavPacket(MAVLinkPacket packet) {
        final byte[] packetData = packet.encodePacket();
        if (!mPacketsToSend.offer(packetData)) {
            NeptusLog.pub().info("Unable to send mavlink packet. Packet queue is full!");
        }
    }

    private void setMAVLinkConnected(boolean state) {
        isMAVLinkConnected = state;
    }

    public boolean isMAVLinkConnected() {
        return isMAVLinkConnected;
    }

    /**
     * Host address to connect
     * @return the tcpHost
     */
    public String getTcpHost() {
        return tcpHost;
    }

    /**
     * Define host address to connect
     * @param tcpHost the address to set
     */
    public void setTcpHost(String tcpHost) {
        this.tcpHost = tcpHost;
    }

    /**
     * Host port to connect to
     * @return the tcpPort
     */
    public int getTcpPort() {
        return tcpPort;
    }

    /**
     * Define host port to connect
     * @param tcpPort the port to set
     */
    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public void setAddressAndPort(String addr, int port) {
        tcpHost = addr;
        tcpPort = port;
    }

    /**
     * @return the toInitiateConnection
     */
    public boolean isToInitiateConnection() {
        return toInitiateConnection;
    }

    /**
     * @param toInitiateConnection the toInitiateConnection to set
     */
    public void initiateConnection(boolean toInitiateConnection) {
        this.toInitiateConnection = toInitiateConnection;
    }

    public static void main(String argv[]) {
        MAVLinkConnection mav = new MAVLinkConnection("10.0.20.125", 9999);
        mav.initiateConnection(true);
        mav.connect();
        mav.send();
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Sending request of parameter list");
        MAVLinkParameters.requestParametersList(mav);
    }

}
