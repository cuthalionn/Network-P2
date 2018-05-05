/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author taha
 */
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
public class RDTClient {
    public static void main(String args[]) throws IOException{
        int sourcePortNumber = Integer.parseInt(args[0]);
        int targetPortNumber = Integer.parseInt(args[1]);
        InetAddress addr = null;
        try{
            addr = InetAddress.getByName("127.0.0.1");
        }
        catch(UnknownHostException e){
            System.out.println(e);
        }
        DatagramSocket ds = null;
        try{
            ds = new DatagramSocket(sourcePortNumber,addr);
            System.out.println("initialized");
        }       
        catch(SocketException e){
            System.out.println(e);
        }
        String mes = "heloo";
        byte[] buf = mes.getBytes(StandardCharsets.US_ASCII);
        byte[] pack = new byte[2 + buf.length];
        pack[0] =0;
        pack[1] = 0;
        for(int i=0;i<buf.length;i++){
            pack[i+2] = buf[i];
        }
        DatagramPacket dp = new DatagramPacket(pack, pack.length,addr,targetPortNumber);
        ds.send(dp);
        ds.receive(dp);
        buf = dp.getData();
        System.out.println("The type of data packet is:"+buf[0]);
        System.out.println("the seq of data packet is:"+buf[1]);
        byte[] message = new byte[buf.length-2];
        for(int i=0;i<buf.length;i++){
            message[i]= buf[i+2];
        }
        byte[] decoded = Base64.getDecoder().decode(message);
        System.out.println(new String(decoded));    // Outputs "HELOO"
        
    }
}
