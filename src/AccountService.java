import java.rmi.Remote;
import java.sql.SQLException;

/**
 * Created by free0u on 9/11/14.
 */
public interface AccountService extends Remote {
    /**
     * Retrieves current balance or zero if addAmount() method was not called before for specified id
     *
     * @param id balance identifier
     */
    Long getAmount(Integer id) throws SQLException;

    /**
     * Increases balance or set if addAmount() method was called first time
     *
     * @param id    balance identifier
     * @param value positive or negative value, which must be added to current balance
     */
    void addAmount(Integer id, Long value) throws SQLException;

    /**
     * Returns count of call of specified function
     *
     * @param id    function identifier
     * @return count of calls
     * @throws SQLException
     */
    public Integer getCountRequestsAll(int id) throws SQLException;

    /**
     * Returns count of call of specified function in the last 'sec' seconds
     *
     * @param id    function identifier
     * @param sec   count of seconds
     * @return count of calls
     * @throws SQLException
     */
    public Integer getCountRequestsPerInterval(int id, int sec) throws SQLException;

    /**
     *  Clears log in database
     *
     * @throws SQLException
     */
    public void clearLogs() throws SQLException;
}
