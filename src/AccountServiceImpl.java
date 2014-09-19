import java.sql.*;
import java.util.HashMap;
import java.util.Map;
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
            con = DriverManager.getConnection(url, userName, userPass);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ResultSet executeQuery(String query) {
        try {
            Statement stmt = con.createStatement();

            boolean status = stmt.execute(query);
            if (status) {
                return stmt.getResultSet();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long getValueById(int id) {
        try (
            Statement stmt = con.createStatement();
            ResultSet rs = executeQuery("select * from store where id = " + id);
        ) {
            if (rs.next()) {
                return rs.getLong("value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void insertValue(int id, Long value) {
        String query = String.format("insert into store values (%d, %d)", id, 0L);
        try (
                Statement stmt = con.createStatement();
        ) {
            boolean status = stmt.execute(query); // TODO change execute to another function
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateValue(int id, Long value) {
        String query = String.format("update store set value = %d where id = %d", value, id);
        try (
                Statement stmt = con.createStatement();
        ) {
            boolean status = stmt.execute(query); // TODO change execute to another function
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    
    /*
    * invariant:
    * if id in a cache then
    *    cache[id] is the same with store[id]
    */


    @Override
    public Long getAmount(Integer id) {
        r.lock();
        try {
            if (cache.containsKey(id)) {
                return cache.get(id);
            }

            Long retValue = getValueById(id);
            if (retValue != null) {
                cache.put(id, retValue);
            } else {
                retValue = 0L;
            }

            return retValue;
        } finally {
            r.unlock();
        }
    }

    @Override
    public void addAmount(Integer id, Long value) {
        w.lock();
        try {
            Long newValue = getValueById(id);
            if (newValue == null) {
                newValue = 0L;
                insertValue(id, 0L);
            }
            newValue += value;

            updateValue(id, newValue);
            cache.put(id, newValue);

        } finally {
            w.unlock();
        }
    }

    public String toString() {
        return cache.toString();
    }
}
