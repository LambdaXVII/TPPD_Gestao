
package servidorgestao;

import java.util.*;
import java.net.*;
import java.io.*;


class AtendeClientesUDP extends Thread {
public static final int MAX_SIZE = 256;
    public static final String TIME_REQUEST = "TIME";
    
    private DatagramSocket socket;
    private DatagramPacket packet; //para receber os pedidos e enviar as respostas
    private boolean debug;

    public AtendeClientesUDP(int listeningPort, boolean debug) throws SocketException 
    {
        socket = null;
        packet = null;
        socket = new DatagramSocket(listeningPort);
        this.debug = debug;
    }
    
    public String waitDatagram() throws IOException
    {
        String request;
        
        if(socket == null){
            return null;
        }
        
        packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
        socket.receive(packet);
        request = new String(packet.getData(), 0, packet.getLength());
        
        if(debug){
            System.out.println("Recebido \"" + request + "\" de " + 
                    packet.getAddress().getHostAddress() + ":" + packet.getPort());
        }
        return request;
    }
    
    
    public void processRequests() throws IOException
    {
        String receivedMsg, timeMsg;
        Calendar calendar;        
        
        if(socket == null){
            return;
        }
        
        if(debug){
            System.out.println("UDP Time Server iniciado...");
        }
        
        while(true){
            
            receivedMsg = waitDatagram();
            
            if(receivedMsg == null){
                continue;
            }
            
            if(!receivedMsg.equalsIgnoreCase(TIME_REQUEST)){
                continue;
            }
            
            calendar = GregorianCalendar.getInstance();
            timeMsg = calendar.get(GregorianCalendar.HOUR_OF_DAY)+":"+ 
                    calendar.get(GregorianCalendar.MINUTE)+":"+calendar.get(GregorianCalendar.SECOND);
            
            packet.setData(timeMsg.getBytes());
            packet.setLength(timeMsg.length());
            
            //O ip e porto de destino ja' se encontram definidos em packet
            socket.send(packet);
            
        }
    }
    
    public void closeSocket()
    {
        if(socket != null){
            socket.close();
        }
    }

}






class AtendeCliente extends Thread
{
    Socket socketToClient;
    int myId;
    
    public AtendeCliente(Socket s, int id)
    {
        socketToClient = s;
        myId = id;        
    }
    
    @Override
    public void run()
    {
        String request = "", timeMsg;
        Calendar calendar;  
        BufferedReader in;
        PrintWriter out;
        
        try{
            out = new PrintWriter(socketToClient.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socketToClient.getInputStream()));

           while( !(request = in.readLine()).equalsIgnoreCase("fim") ){

            if(request == null){ //EOF
                socketToClient.close();
                System.out.println("<Thread_" + myId + "> "
                        + "Ligacacao encerrada.");
                return;
            }

            System.out.println("<Thread_" + myId + 
                    "> Recebido \"" + request.trim() + "\" de " + 
                    socketToClient.getInetAddress().getHostAddress() + ":" + 
                    socketToClient.getPort());


            if(request.equalsIgnoreCase(ServidorGestao.TIME_REQUEST)){

                //Constroi a resposta terminando-a com uma mudanca de linha
                calendar = GregorianCalendar.getInstance();
                timeMsg = calendar.get(GregorianCalendar.HOUR_OF_DAY)+":"+ 
                        calendar.get(GregorianCalendar.MINUTE)+":"+
                        calendar.get(GregorianCalendar.SECOND);

                
                //Envia a resposta ao cliente
                out.println(timeMsg);
                out.flush();
            }
           }
        }catch(IOException e){
            System.out.println("<Thread_" + myId + "> Erro na comunicação como o cliente " + 
                    socketToClient.getInetAddress().getHostAddress() + ":" + 
                        socketToClient.getPort()+"\n\t" + e);
        }finally{
            try{
                socketToClient.close();
            }catch(IOException e){}
        }
    }
}

public class ServidorGestao {
 public static final int MAX_SIZE = 256;
    public static final String TIME_REQUEST = "TIME";
    
    private ServerSocket socket;

    public ServidorGestao(int listeningPort) throws IOException
    {        
        socket = new ServerSocket(listeningPort);
    }
    
    public final void processRequests() throws IOException
    {
        int threadId = 1;
        Socket toClientSocket;
        
        if(socket == null){
            return;
        }
        
        System.out.println("Concurrent TCP Time Server iniciado no porto " + socket.getLocalPort() + " ...");
        
        
        while(true){     
            toClientSocket = socket.accept();            
            new AtendeCliente(toClientSocket, threadId++).start();  
            
        }
    }
    
   
           
    public static void main(String[] args) throws IOException 
    {                
        if(args.length != 1){
            System.out.println("Sintaxe: java TcpTimeServer listeningPort");
            return;
        }
        // 
        System.out.println("Endereço IP: "+InetAddress.getLocalHost().toString()); 
        System.out.println("Porto TCP para os clientes: "+args[0]);
        
      
        // Trata pedidos UDP dos Servidores de Jogo
        AtendeClientesUDP atendeUDP = new AtendeClientesUDP(8000,true);  
        
       
         try{
            
            atendeUDP.processRequests();
            
        }catch(NumberFormatException e){
            System.out.println("O porto de escuta deve ser um inteiro positivo.");
        }catch(SocketException e){
            System.out.println("Ocorreu um erro ao nível do socket UDP:\n\t"+e);
        }catch(IOException e){
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t"+e);
        }finally{
            if(atendeUDP != null){
                atendeUDP.closeSocket();
            }
         }
         
         
        // Instancia Servidor de Gestão
        //ServidorGestao tcpTimeServer = new ServidorGestao(Integer.parseInt(args[0]));
       
        // Trata de pedidos TCP -> vindos dos clientes
       // tcpTimeServer.processRequests();            
    }
}
