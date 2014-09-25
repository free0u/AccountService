import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class Client implements Runnable {
    private AccountService serv;
    private List<Integer> ids;
    private int type; // 0 - getAmount, 1 - addAmount
    private Random rand;
    private int countRequests;

    public Client(AccountService serv, List<Integer> ids, int type, int countRequests) {
        this.serv = serv;
        this.ids = ids;
        this.type = type;
        this.countRequests = countRequests;
        rand = new Random();
    }

    @Override
    public void run() {
        int ind = rand.nextInt(ids.size());
        int id = ids.get(ind);
        try {
            for (int i = 0; i < countRequests; i++) {
                Thread.sleep(rand.nextInt(300));
                if (type == 0) { // getAmount
                    Long result = serv.getAmount(id);
//                    System.out.println(String.format("getAmount(%d): %d", id, result));
                } else { // addAmount
                    int nv = rand.nextInt(10);
                    serv.addAmount(id, (long) nv);
//                    System.out.println(String.format("addAmount(%d, %d)", id, nv));
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws SQLException {
        if (args.length < 3) {
            System.out.println("Usage: program rCount wCount 'list of ids' [host]'");
            return;
        }

        int rCount = Integer.parseInt(args[0]);
        int wCount = Integer.parseInt(args[1]);

        StringTokenizer st = new StringTokenizer(args[2], ",");
        List<Integer> ids = new ArrayList<>();
        while (st.hasMoreTokens()) {
            ids.add(Integer.parseInt(st.nextToken()));
        }

        String host = (args.length < 4) ? null : args[3];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            AccountService serv = (AccountService) registry.lookup("AccountService");

            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < rCount; i++) {
                Client client = new Client(serv, ids, 0, 100);
                threads.add(new Thread(client));
            }
            for (int i = 0; i < wCount; i++) {
                Client client = new Client(serv, ids, 1, 100);
                threads.add(new Thread(client));
            }

            for (Thread t : threads) {
                t.start();
            }

            while (true) {
                Thread.sleep(1000);
                int interval = 1;
                int cntGetAmount = serv.getCountRequestsPerInterval(1, interval);
                int cntAddAmount = serv.getCountRequestsPerInterval(2, interval);
                System.out.println(String.format("Stat (for last %d sec): getAmount calls=%d; addAmount calls=%d", interval, cntGetAmount, cntAddAmount));

                boolean alive = false;
                for (Thread t : threads) {
                    if (t.isAlive()) {
                        alive = true;
                        break;
                    }
                }
                if (!alive) {
                    break;
                }
            }

            int cntGetAmountTotal = serv.getCountRequestsAll(1);
            int cntAddAmountTotal = serv.getCountRequestsAll(2);
            System.out.println(String.format("Stat (total): getAmount calls=%d; addAmount calls=%d", cntGetAmountTotal, cntAddAmountTotal));
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
