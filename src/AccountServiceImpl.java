import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AccountServiceImpl implements AccountService {
    private Map<Integer, Long> cache = new HashMap<>();
    private Map<Integer, ReentrantReadWriteLock> locks = new HashMap<>();
    private boolean inRead, inWrite;

    private Connection con = null;

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
                "id serial primary key," +
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

    private Long getValueById(int id) throws SQLException {
        try (
            ResultSet rs = executeQuery("select * from store where id = " + id);
        ) {
            if (rs.next()) {
                return rs.getLong("value");
            }
        }
        return null;
    }

    private void insertValue(int id, Long value) throws SQLException {
        String query = String.format("insert into store values (%d, %d)", id, 0L);
        try (
                Statement stmt = con.createStatement();
        ) {
            int status = stmt.executeUpdate(query);
        }
    }

    private void updateValue(int id, Long value) throws SQLException {
        String query = String.format("update store set value = %d where id = %d", value, id);
        try (
                Statement stmt = con.createStatement();
        ) {
            int status = stmt.executeUpdate(query);
        }
    }


    private void insertTimestamp(int id) throws SQLException {
        String query = String.format("insert into functionCalls(functionsId, ts) values (%d, '%s')",
                id,
                new Timestamp(new java.util.Date().getTime()));
        try (
                Statement stmt = con.createStatement();
        ) {
            int status = stmt.executeUpdate(query);
        }
    }

    public Integer getCountRequestsAll(int id) throws SQLException {
        try (
                ResultSet rs = executeQuery("select count(*) from FunctionCalls where functionsId = " + id);
        ) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return null;
    }

    public Integer getCountRequestsPerInterval(int id, int sec) throws SQLException {
        Timestamp ts = new Timestamp(new java.util.Date().getTime() - sec * 1000L);
        String query = String.format("select count(*) from FunctionCalls where functionsId = %d and ts >= '%s'", id, ts);
        try (

                ResultSet rs = executeQuery(query);
        ) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return null;
    }


    public void clearLogs() throws SQLException {
        String query = "delete from functionCalls";
        try (
                Statement stmt = con.createStatement();
        ) {
            int status = stmt.executeUpdate(query);
        }
    }

    
    /*
    * invariant for getAmount and addAmount:
    * if id in a cache then
    *    cache[id] is the same with store[id]
    */


    @Override
    public Long getAmount(Integer id) throws SQLException {
        insertTimestamp(1);
        if (!locks.containsKey(id)) {
            locks.put(id, new ReentrantReadWriteLock());
        }
        locks.get(id).readLock().lock();

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
            locks.get(id).readLock().unlock();
        }
    }

    @Override
    public void addAmount(Integer id, Long value) throws SQLException {
        insertTimestamp(2);
        if (!locks.containsKey(id)) {
            locks.put(id, new ReentrantReadWriteLock());
        }
        locks.get(id).writeLock().lock();

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
            locks.get(id).writeLock().unlock();
        }
    }

    public String toString() {
        return cache.toString();
    }

    public static void main(String args[]) {
        try {
            AccountServiceImpl obj = new AccountServiceImpl();
            AccountService stub = (AccountService) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("AccountService", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
