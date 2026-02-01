# Financial Engine - Sistem de Execuție a Ordinelor de Tranzacționare

## Descriere

Sistem educațional de tranzacționare în Java care simulează execuția ordinelor financiare cu:

- **5 instrumente financiare** cu prețuri dinamice
- **Concurență** bazată pe ExecutorService și ScheduledExecutorService
- **Audit periodic** la fiecare 2 secunde
- **5 clienți simulatori** (boți) care trimit ordine
- **Persistență** în fișiere text

## Structura Proiectului

```
src/
├── Main.java                    # Punct de intrare - orchestrează sistemul
├── model/
│   ├── Order.java              # Modelul unui ordin
│   ├── Instrument.java         # Instrument financiar
│   ├── Execution.java          # Execuție ordin (pentru logging)
│   ├── OrderType.java          # Enum: BUY_LIMIT, SELL_LIMIT
│   └── OrderStatus.java        # Enum: PENDING, EXECUTED, CANCELLED, REJECTED
├── server/
│   ├── TradingEngine.java      # Motorul principal
│   ├── LiquidityManager.java   # Gestionare lichiditate
│   └── AuditService.java       # Audit periodic și execuție
├── client/
│   └── TradingBot.java         # Simulator client
└── util/
    ├── PriceSimulator.java     # Simulare prețuri (model stochastic)
    ├── IdGenerator.java        # Generare ID-uri unice
    └── FileLogger.java         # Logging thread-safe în fișiere
```

## Cum Funcționează

### 1. Instrumente Financiare

Sistemul gestionează 5 instrumente: AAPL, GOOGL, MSFT, TSLA, AMZN.

Fiecare are:

- Preț curent (actualizat dinamic)
- Lichiditate maximă (V_max)
- Volatilitate (sigma)
- Trend (mu)

Prețul evoluează conform:

```
newPrice = prevPrice + mu * dt + sigma * sqrt(dt) * epsilon
```

unde epsilon ~ N(0,1)

### 2. Tipuri de Ordine

- **BUY_LIMIT**: Cumpără dacă prețul curent ≤ preț limită
- **SELL_LIMIT**: Vinde dacă prețul curent ≥ preț limită

### 3. Fluxul unui Ordin

```
Client trimite ordin
    ↓
Verificare lichiditate
    ↓
Da: status = PENDING → Coadă    |    Nu: status = REJECTED
    ↓
Client primește Future<OrderStatus>
    ↓
Audit Service (la 2 sec):
  - Verifică expirare (30 sec) → CANCELLED
  - Verifică condiție execuție → EXECUTED
    ↓
Client notificat prin Future
```

### 4. Audit Periodic (la 2 secunde)

- Actualizează prețurile instrumentelor
- Procesează ordine pending:
  - Anulează ordinele expirate (> 30 sec)
  - Execută ordinele care îndeplinesc condițiile
- Verifică integritatea lichidității
- Scrie log-uri în fișiere

### 5. Persistență

Sistemul scrie în 4 fișiere text:

- **orders.txt**: Toate ordinele primite
- **executions.txt**: Ordinele executate
- **cancellations.txt**: Ordinele anulate (expirate)
- **audit_log.txt**: Log-uri periodice cu statusul sistemului

## Compilare și Rulare

### Compilare

```bash
cd "c:\Users\tudor\Desktop\banking app\src"
javac -d ../bin Main.java model/*.java server/*.java client/*.java util/*.java
```

### Rulare

```bash
cd "c:\Users\tudor\Desktop\banking app\bin"
java Main
```

### Rulare alternativă (fără compilare separată)

```bash
cd "c:\Users\tudor\Desktop\banking app\src"
javac Main.java model/*.java server/*.java client/*.java util/*.java
java Main
```

## Parametri Configurabili (în Main.java)

```java
private static final int NUM_INSTRUMENTS = 5;  // Număr instrumente
private static final int NUM_THREADS = 4;       // Thread-uri în pool
private static final int NUM_CLIENTS = 5;       // Număr clienți (boți)
private static final int RUNTIME_MINUTES = 3;   // Durată rulare
```

## Exemple de Output

### Console Output

```
╔════════════════════════════════════════════════════╗
║   FINANCIAL ENGINE - Sistem Execuție Ordine       ║
╚════════════════════════════════════════════════════╝

✓ Instrumente create: 5
  Instrument[AAPL, price=150.00, maxLiq=1000.00]
  Instrument[GOOGL, price=2800.00, maxLiq=800.00]
  ...

Trading Engine pornit!
Audit Service pornit (interval: 2 secunde)

✓ Boți porniți: 5

[CLIENT-1] Trimite ordin: BUY_LIMIT AAPL 45.23 @ 155.00 (curent: 150.00)
✓ ORD-1 ACCEPTAT în coadă

--- AUDIT CYCLE: 2026-02-01 14:30:02 ---
Prețuri actualizate:
  AAPL: 151.23
  GOOGL: 2805.67
  ...
✅ EXECUTAT: ORD-1 la prețul 151.23
```

### Fișiere Generate

- **orders.txt**: Lista tuturor ordinelor
- **executions.txt**: Detalii execuții
- **cancellations.txt**: Ordine anulate
- **audit_log.txt**: Snapshot-uri periodice

## Caracteristici Tehnice

### Concurență

- **ExecutorService** cu pool de P thread-uri pentru procesare ordine
- **ScheduledExecutorService** pentru audit periodic (2 secunde)
- **CompletableFuture** pentru notificare asincronă clienți
- **BlockingQueue** thread-safe pentru ordine pending
- **ConcurrentHashMap** pentru lichiditate

### Thread Safety

- Toate operațiile pe lichiditate sunt sincronizate
- Scrierea în fișiere este thread-safe (synchronized)
- Prețurile instrumentelor folosesc volatile și synchronized

### Design Simplu

- Fără framework-uri externe
- Doar Java standard (java.util.concurrent, java.io)
- Cod clar, bine comentat
- Ușor de extins și explicat

## Extensii Posibile

1. **Mai multe tipuri de ordine**: MARKET, STOP_LOSS
2. **Matching engine**: Ordine contra între clienți
3. **Metrici în timp real**: Profit/loss per client
4. **Persistență în bază de date**: H2 sau SQLite
5. **API REST**: Expunere prin HTTP
6. **WebSocket**: Notificări în timp real
7. **Dashboard web**: Vizualizare status sistem

## Notițe Importante

- Sistemul rulează **3 minute** apoi se oprește automat
- Auditul rulează la fiecare **2 secunde**
- Ordinele expiră după **30 secunde**
- Boții trimit ordine la fiecare **1 secundă**
- Lichiditatea este verificată la fiecare plasare ordin

## Licență

Proiect educațional - utilizare liberă.
