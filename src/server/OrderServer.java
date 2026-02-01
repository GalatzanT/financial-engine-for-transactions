package server;

import model.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Server TCP care ascultă pe un port și procesează ordine de la clienți.
 * Fiecare conexiune client este gestionată într-un thread separat.
 */
public class OrderServer {
    private final int port;
    private final TradingEngine engine;
    private ServerSocket serverSocket;
    private final ExecutorService clientHandlerPool;
    private volatile boolean running = false;
    
    public OrderServer(int port, TradingEngine engine) {
        this.port = port;
        this.engine = engine;
        // Pool de thread-uri pentru a gestiona conexiunile clienților
        this.clientHandlerPool = Executors.newCachedThreadPool();
    }
    
    /**
     * Pornește serverul TCP și acceptă conexiuni de la clienți.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("✓ Order Server pornit pe portul " + port);
            
            // Thread separat pentru acceptarea conexiunilor
            new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        // Gestionează fiecare client într-un thread separat
                        clientHandlerPool.submit(new ClientHandler(clientSocket));
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Eroare acceptare client: " + e.getMessage());
                        }
                    }
                }
            }, "ServerAcceptThread").start();
            
        } catch (IOException e) {
            System.err.println("Eroare pornire server: " + e.getMessage());
        }
    }
    
    /**
     * Oprește serverul și închide toate conexiunile.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientHandlerPool.shutdown();
            if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientHandlerPool.shutdownNow();
            }
            System.out.println("Order Server oprit!");
        } catch (Exception e) {
            System.err.println("Eroare oprire server: " + e.getMessage());
        }
    }
    
    /**
     * Handler pentru fiecare conexiune client.
     * Protocol: SUBMIT|clientId|instrumentId|orderType|volume|limitPrice
     * Răspuns: ACCEPTED|orderId sau REJECTED|reason
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String request;
                // Procesează cereri de la client până când se deconectează
                while ((request = in.readLine()) != null) {
                    String response = processRequest(request);
                    out.println(response);
                }
            } catch (IOException e) {
                // Client deconectat - normal
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignoră erori la închidere
                }
            }
        }
        
        /**
         * Procesează o cerere de la client.
         * Format: SUBMIT|clientId|instrumentId|orderType|volume|limitPrice
         */
        private String processRequest(String request) {
            try {
                String[] parts = request.split("\\|");
                
                if (parts.length < 1) {
                    return "ERROR|Format invalid";
                }
                
                String command = parts[0];
                
                if ("SUBMIT".equals(command)) {
                    return handleSubmitOrder(parts);
                } else if ("PING".equals(command)) {
                    return "PONG";
                } else {
                    return "ERROR|Comandă necunoscută: " + command;
                }
                
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }
        
        /**
         * Gestionează comanda SUBMIT pentru plasarea unui ordin.
         * Format: SUBMIT|clientId|instrumentId|orderType|volume|limitPrice
         */
        private String handleSubmitOrder(String[] parts) {
            if (parts.length != 6) {
                return "ERROR|Format SUBMIT invalid. Așteptat: SUBMIT|clientId|instrumentId|orderType|volume|limitPrice";
            }
            
            try {
                String clientId = parts[1];
                String instrumentId = parts[2];
                String orderTypeStr = parts[3];
                double volume = Double.parseDouble(parts[4]);
                double limitPrice = Double.parseDouble(parts[5]);
                
                // Găsește instrumentul
                Instrument instrument = engine.getInstruments().values().stream()
                    .filter(i -> i.getId().equals(instrumentId))
                    .findFirst()
                    .orElse(null);
                
                if (instrument == null) {
                    return "REJECTED|Instrument inexistent: " + instrumentId;
                }
                
                // Parse order type
                OrderType orderType;
                try {
                    orderType = OrderType.valueOf(orderTypeStr);
                } catch (IllegalArgumentException e) {
                    return "REJECTED|Tip ordin invalid: " + orderTypeStr;
                }
                
                // Creează și trimite ordinul la engine
                Order order = new Order(
                    util.IdGenerator.generateOrderId(),
                    clientId,
                    instrument,
                    orderType,
                    volume,
                    limitPrice
                );
                
                // submitOrder returnează CompletableFuture<OrderStatus>
                // Pentru simplitate, returnăm ACCEPTED dacă ordinul a fost pus în coadă
                // Statusul real va fi procesat de AuditService
                CompletableFuture<OrderStatus> future = engine.submitOrder(order);
                
                // Verificăm statusul inițial al ordinului (PENDING sau REJECTED)
                OrderStatus initialStatus = order.getStatus();
                
                if (initialStatus == OrderStatus.PENDING) {
                    return "ACCEPTED|" + order.getOrderId();
                } else {
                    return "REJECTED|" + initialStatus;
                }
                
            } catch (NumberFormatException e) {
                return "ERROR|Volume sau limitPrice invalid";
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }
    }
}
