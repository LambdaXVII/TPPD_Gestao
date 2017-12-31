
package servidorgestao;

import java.util.*;
import java.net.*;
import java.io.*;



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
        String request, timeMsg;
        Calendar calendar;  
        BufferedReader in;
        PrintWriter out;
        
        try{
            out = new PrintWriter(socketToClient.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socketToClient.getInputStream()));

            request = in.readLine();

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
                
        ServidorGestao tcpTimeServer = new ServidorGestao(Integer.parseInt(args[0]));
        tcpTimeServer.processRequests();            
    }
}
