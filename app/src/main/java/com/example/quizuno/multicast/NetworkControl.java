package com.example.quizuno.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

public class NetworkControl implements Runnable{

    private MulticastSocket msj;
    private InetAddress groupIp;
    private int groupPort;
    private int id;
    private boolean identified;


    public NetworkControl() {
        try {
            id = -1;
            identified = false;
            groupPort = 5000;
            msj = new MulticastSocket(groupPort);
            groupIp = InetAddress.getByName("228.0.0.1"); // 224.0.0.0 to 239.255.255.255 CLASS D IP
            msj.joinGroup(groupIp);
            toIdentificate();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toIdentificate() throws IOException {
        //espera durante 600 mls algun mensaje entrante
        msj.setSoTimeout(600);

        //mensaje de saludo
        byte[] buf = "id-new".getBytes();
        DatagramPacket hiPacket = new DatagramPacket(buf, buf.length, groupIp, groupPort);
        msj.send(hiPacket);
        //
        while(!identified) {
            try{
                byte[] tempBuf = new byte[32];
                DatagramPacket externalResponsePacket = new DatagramPacket(tempBuf, tempBuf.length);
                msj.receive(externalResponsePacket);
                //convierte los bytes en String y elimina los espacios blancos
                String message = new String(externalResponsePacket.getData(), 0, externalResponsePacket.getLength()).trim();



                if(message.contains("id-im:")) {
                    int incomingID = Integer.parseInt((message.split(":")[1]));
                    if(incomingID >= id) {
                        id = incomingID+1;
                    }
                }
            }catch (SocketTimeoutException e) {
                System.out.println("no more messages");
                if(id == -1) {
                    id = 0;
                }
                msj.setSoTimeout(0);
                identified = true;
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if(identified == true){
                    receivePacket();
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receivePacket() throws IOException {
        byte[] buf = new byte[128];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        msj.receive(p);
        String message = new String(p.getData(), 0, p.getLength()).trim();
        MessageControl(message);
        System.out.println(message);
    }

    private void MessageControl(String message) {
        if(message.contains("id-new")) {
            // new user
            sendPacket("id-im:"+id);
        }
        // ... new types
    }

    public void sendPacket(String outgoingMessage) {
        try {
            byte[] buf = outgoingMessage.getBytes();
            DatagramPacket outgoingPacket = new DatagramPacket(buf, buf.length, groupIp, groupPort);
            msj.send(outgoingPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }
}
