
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;

public class RDTClient {

    final static int MAX_DATA_LENGTH = 64;
    final static int TYPE_DATA = 0;
    final static int TYPE_ACK = 1;
    final static int TYPE_END = 2;
    final static int TIME_OUT = 2000;

    static byte sequenceNumber = 0;
    static int targetPort;
    static BufferedInputStream bufferedInputStream;
    static DatagramSocket datagramSocket;
    static InetAddress inetAddress;
    static String fileName;
    static FileOutputStream outputStream;

    static int totalTransmission = 0;
    static int totalMessage = 0;

    private static void setup(int sourcePort) throws Exception{
        //Prepare Reader
        FileInputStream fileInputStream = new FileInputStream(fileName);
        bufferedInputStream = new BufferedInputStream(fileInputStream);

        //Prepare Writer
        String alteredFileName = fileName.substring(0,fileName.indexOf("."));
        outputStream = new FileOutputStream(alteredFileName + "_altered.txt");


        //Prepare DatagramSocket
        inetAddress = InetAddress.getByName("127.0.0.1");
        datagramSocket = new DatagramSocket(sourcePort, inetAddress);
    }

    private static void send() throws Exception{
        ArrayList<Byte> arrayList;
        while(true){
            int nextByte;
            arrayList = new ArrayList<>();
            for(int index = 0; index < MAX_DATA_LENGTH; index++){
                nextByte = bufferedInputStream.read();
                if (nextByte != -1){
                    arrayList.add((byte)nextByte);
                } else {
                    break;
                }
            }
            Byte[] objectArray = new Byte[arrayList.size()];
            arrayList.toArray(objectArray);
            byte[] primitiveArray = new byte[MAX_DATA_LENGTH];
            int index = 0;
            for(Byte b: objectArray)
                primitiveArray[index++] = b;
            switch (arrayList.size()){
                case 0:
                    sendEnd();
                    return;
                case MAX_DATA_LENGTH:
                    sendData(primitiveArray);
                    break;
                default:
                    sendData(primitiveArray);
                    sendEnd();
                    return;
            }
        }
    }

    private static void receive() throws Exception{
        sequenceNumber = 0;
        DatagramPacket datagramPacket;
        byte[] buffer;
        while(true){
            buffer = new byte[66];
            datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, targetPort);
            datagramSocket.setSoTimeout(0);
            datagramSocket.receive(datagramPacket);
            buffer = datagramPacket.getData();
            switch (buffer[0]){
                case TYPE_DATA:
                    if (buffer[1] == sequenceNumber){
                        totalMessage++;
                        totalTransmission++;
                        sendAck();
                        writeToFile(Arrays.copyOfRange(buffer, 2, datagramPacket.getLength()));
                    } else {
                        totalTransmission++;
                        increaseSequenceNumber();
                        sendAck();
                    }
                    break;
                case TYPE_END:
                    if (buffer[1] == sequenceNumber){
                        totalMessage++;
                        totalTransmission++;
                        sendAck();
                    } else {
                        throw new Exception("Unexpected Sequence Number For Emd!");
                    }
                    return;
            }
        }
    }

    private static void close() throws Exception{
        bufferedInputStream.close();
        outputStream.close();
        datagramSocket.close();
    }

    private static void sendAck() throws Exception{
        byte[] packet = new byte[2];
        packet[0] = TYPE_ACK;
        packet[1] = sequenceNumber;
        DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, inetAddress, targetPort);
        datagramSocket.send(datagramPacket);
        increaseSequenceNumber();
    }

    private static void writeToFile(byte[] data) throws Exception{
        outputStream.write(data);
    }

    private static void sendEnd() throws Exception{
        byte[] packet = new byte[2];
        packet[0] = TYPE_END;
        packet[1] = sequenceNumber;
        sendPacket(packet);
    }

    private static void sendData(byte[] data) throws Exception{
        byte[] packet = new byte[data.length + 2];
        packet[0] = TYPE_DATA;
        packet[1] = sequenceNumber;
        System.arraycopy(data, 0, packet, 2, data.length);
        sendPacket(packet);
    }

    private static void sendPacket(byte[] packet) throws Exception{
        totalTransmission++;
        totalMessage++;
        DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, inetAddress, targetPort);
        datagramSocket.send(datagramPacket);
        increaseSequenceNumber();
        byte[] buffer = new byte[2];
        datagramPacket = new DatagramPacket(buffer, buffer.length, inetAddress, targetPort);
        datagramSocket.setSoTimeout(TIME_OUT);
        try {
            datagramSocket.receive(datagramPacket);
            buffer = datagramPacket.getData();
            if (buffer[0] != 1){
                throw new Exception("Unexpected Message Type!");
            }
            if (buffer[1] != previousSequenceNumber()){
                throw new Exception("Unexpected Sequence Number!");
            }
        } catch (SocketTimeoutException socketTimeoutException){
            totalMessage--;
            increaseSequenceNumber();
            sendPacket(packet);
        }
    }

    private static void increaseSequenceNumber(){
        sequenceNumber++;
        if (sequenceNumber == 2){
            sequenceNumber = 0;
        }
    }

    private static int previousSequenceNumber(){
        int previousSequenceNumber = sequenceNumber - 1;
        return (previousSequenceNumber == -1) ? 1 : previousSequenceNumber;
    }

    public static void main(String args[]) throws Exception{
        //Get parameters
        int sourcePort = Integer.parseInt(args[0]);
        targetPort = Integer.parseInt(args[1]);
        fileName = args[2];

        setup(sourcePort);
        send();
        receive();
        close();

        System.out.printf(">>Transmission complete: Total number of  messages: %d, Total number of messages sent: %d", totalMessage, totalTransmission);
    }
}