/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

/**
 *
 * @author Diego
 */
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ChatServer extends JFrame {
    
    JTextArea jtaChatLog = new JTextArea();
    
    Semaphore semaphore = new Semaphore(30);
    Socket[] clientSockets = new Socket[100];
    Buffer messageQueue = new Buffer(10);
    
    private static class Buffer{
        private static final int CAPACITY = 10;
        private static int capacity = CAPACITY;
        private LinkedList<String> queue = new LinkedList<>();
        
        public Buffer(int capacity){
            this.capacity = capacity;
        }
        
        private static Lock lock = new ReentrantLock();
        private static Condition notEmpty = lock.newCondition();
        private static Condition notFull = lock.newCondition();
        
        public int size(){
            return queue.size();
        }
        
        public void write(String value){
            lock.lock();
            try{
                while(queue.size() >= CAPACITY){
                    
                    notFull.await();
                }
                queue.offer(value);
                notEmpty.signal();
                
            }catch(InterruptedException ex){
                ex.printStackTrace();
            }finally{
                lock.unlock();
            }
        }
        
        public String read(){
            String message = "";
            lock.lock();
            try{
                while(queue.isEmpty()){
                    
                    notEmpty.await();
                }
                
                message = queue.remove();
                notFull.signal();
                
                
            }catch(InterruptedException ex){
                ex.printStackTrace();
            }finally{
                lock.unlock();
                return message;
            }
        }
    }
    
    public ChatServer(){
        setTitle("Chat Server");
        setSize( 500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        add(new JScrollPane(jtaChatLog) );
        setVisible(true);
        
        try{
            ServerSocket serverSocket = new ServerSocket(8008);
            jtaChatLog.append("Server started at " + new Date() + "\n");
            
            Consumer consumerPool = new Consumer();
            new Thread(consumerPool).start();
            
            while(true){
                Socket clientSocket = serverSocket.accept();
                for(int i = 0; i < clientSockets.length; i = i + 1){
                    if( clientSockets[i] == null || clientSockets[i].isClosed() ){
                        Producer client = new Producer(clientSocket);
                        new Thread(client).start();
                        clientSockets[i] = clientSocket;
                        break;
                    }
                }
                
            }
            
            
        }catch(IOException ioEx){
            System.err.println(ioEx + "\n");
        }
        
        
    }
    
    private class Consumer implements Runnable{
        
        
        public Consumer(){
            
        }
        
        
        
        @Override
        public void run(){
            
                //consume all the messages
                while(jtaChatLog != null){
                    
                    try{
                    while( messageQueue.size() == 0 ){
                        for(int i = 0; i < 100; i = i + 1){
                            if( clientSockets[i] .isClosed() ){
                                clientSockets[i] = null;
                            }
                        }
                        Thread.sleep(10);
                    }
                    }catch(Exception ex){
                    }
                    
                    
                    String message = messageQueue.read();
                    
                    try{
                        
                        ObjectOutputStream outputToClient;
                        for(int i = 0; i < 100; i = i + 1){
                            if( clientSockets[i] != null && ! clientSockets[i].isClosed() ){
                                outputToClient = new ObjectOutputStream( clientSockets[i].getOutputStream() );
                                outputToClient.writeObject(message);
                            }
                        }
                        
                        
                    }catch(IOException ioEx){
                        
                    }
                }
            }
        
    }
    
    
    private class Producer implements Runnable{
        Socket clientSocket;
        
        public Producer(Socket clientSocket){
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run(){
            while(jtaChatLog != null){
                try{
                    ObjectInputStream inputFromClient = new ObjectInputStream( clientSocket.getInputStream() );
                    String message = (String)( inputFromClient.readObject() );
                    
                    
                    
                    messageQueue.write(message);
                    
                    synchronized(jtaChatLog){
                        jtaChatLog.append(message);
                    }
                    
                }catch(IOException ioEx){
                    
                }catch(ClassNotFoundException clEx){
                    
                }
            }
            try{
                clientSocket.close();
            }catch(Exception ex){
                
            }
        }
    }
    
    
    public static void main(String[] args) {
        new ChatServer();
    }
    
}
