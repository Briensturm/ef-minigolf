package com.aapeli.connection;

import com.aapeli.tools.Tools;

import java.util.Vector;

class GamePacketQueue implements Runnable {

    private Connection conn;
    private ConnListener connListener;
    private Vector<String> packets;
    private boolean running;
    private Thread thread;


    protected GamePacketQueue(Connection conn, ConnListener connListener) {
        this.conn = conn;
        this.connListener = connListener;
        this.packets = new Vector<>();
        this.running = true;
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void run() {
        while (true) {
            try {
                Tools.sleep(50L);

                String packet;
                while ((packet = this.nextGamePacket()) != null) {
                    this.connListener.dataReceived(packet);
                }

                if (this.running) {
                    continue;
                }
            } catch (Exception e) {
                this.running = false;
                this.conn.handleCrash();
            }

            return;
        }
    }

    protected synchronized void addGamePacket(String command) {
        this.packets.addElement(command);
    }

    protected void stop() {
        this.running = false;
    }

    private synchronized String nextGamePacket() {
        if (!this.packets.isEmpty() && this.running) {
            String var1 = this.packets.elementAt(0);
            this.packets.removeElementAt(0);
            return var1;
        } else {
            return null;
        }
    }
}
