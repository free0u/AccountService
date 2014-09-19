import java.sql.*;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by free0u on 9/11/14.
 */
public class Tester implements Runnable {
    int ind;
    AccountService serv;
    int type;
    // type: 0 - read, 1 - write

    public Tester(int ind, AccountService serv, int type) {
        this.ind = ind;
        this.serv = serv;
        this.type = type;
    }

    @Override
    public void run() {
        Random rand = new Random();
        if (type == 0) {
            serv.getAmount(0);
        } else {
            serv.addAmount(0, rand.nextLong());
        }
    }

    public static void main(String[] args) {
        AccountService serv = new AccountServiceImpl();
        for (int i = 0; i < 10; i++) {
            Tester tester = new Tester(i, serv, 1);
            Thread thread = new Thread(tester);
            thread.start();
        }
    }
}