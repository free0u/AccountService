import javax.xml.transform.Result;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by free0u on 9/11/14.
 */
public class AccountServiceImpl implements AccountService {
    Map<Integer, Long> cache = new HashMap<Integer, Long>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();
    boolean inRead, inWrite;

    Connection con = null;

    public AccountServiceImpl() {
        inRead = false;
        inWrite = false;

        initDBConnection("accountservice", "free0u", "");

        recreateDatabaseScheme();
    }

    private void recreateDatabaseScheme() {
        executeQuery("drop table if exists Store;");
        executeQuery("drop table if exists Functions cascade;");
        executeQuery("drop table if exists FunctionCalls;");

        executeQuery("create table Functions(" +
                "id integer primary key," +
                "name varchar(30)" +
                ");");
        executeQuery("insert into functions values" +
                "(1, 'getAmount')," +
                "(2, 'setAmount');");

        executeQuery("create table FunctionCalls (" +
                "id integer primary key," +
                "functionsId integer references Functions(id)," +
                "ts timestamp" +
                ");");

        executeQuery("create table Store(" +
                "id integer primary key," +
                "value bigint);");
    }

    private void initDBConnection(String dbName, String userName, String userPass) {
        String url = String.format("jdbc:postgresql://127.0.0.1/%s", dbName);

        try {
            con = DriverManager.getConnection(url,userName,userPass);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ResultSet executeQuery(String query) {
        try (Statement stmt = con.createStatement()) {

            boolean status = stmt.execute(query);
            if (status) {
                return stmt.getResultSet();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Long getAmount(Integer id) {
        r.lock();
        try {
            if (inRead) {
                System.out.println("it's not first read");
            }
            inRead = true;
            try {
                Random r = new Random();
                Thread.sleep(r.nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("read begin");

            if (inWrite) {
                try {
                    throw new Exception("already writing");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            long retValue = 0L;
            if (cache.containsKey(id)) {
                retValue = cache.get(id);
            }
            System.out.println("read end");
            inRead = false;
            return retValue;
        } finally {
            r.unlock();
        }
    }

    @Override
    public void addAmount(Integer id, Long value) {
        w.lock();
        try {
            if (inWrite) {
                System.out.println("it's not first write");
            }
            inWrite = true;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (inRead) {
                try {
                    throw new Exception("already reading");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("write begin");
            cache.put(id, value);
            System.out.println("write end");
            inWrite = false;
        } finally {
            w.unlock();
        }
    }

    public void foo() {

    }

    public String toString() {
        return cache.toString();
    }
}
