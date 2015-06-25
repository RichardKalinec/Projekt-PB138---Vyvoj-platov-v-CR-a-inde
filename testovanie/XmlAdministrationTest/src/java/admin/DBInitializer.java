package admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marek
 */
public class DBInitializer
{
    public static void main(String[] args)
    {
        try
        {
            Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Test", "test", "test");
            Statement st = con.createStatement();
            st.execute("CREATE TABLE ADMIN (USERNAME VARCHAR(50), PASSWORD VARCHAR(50))");
            st.execute("INSERT INTO ADMIN (USERNAME, PASSWORD) VALUES ('admin','admin')");
            st.execute("INSERT INTO ADMIN (USERNAME, PASSWORD) VALUES ('ducus','heslo123')");
            st.execute("INSERT INTO ADMIN (USERNAME, PASSWORD) VALUES ('jergus','bakalar')");
            
            st.execute("CREATE TABLE FILE (FILENAME VARCHAR(256), UPLOADED TIMESTAMP)");
            st.execute("INSERT INTO FILE (FILENAME, UPLOADED) VALUES ('Subor1.xml',CURRENT_TIMESTAMP)");
            st.execute("INSERT INTO FILE (FILENAME, UPLOADED) VALUES ('File1.txt',CURRENT_TIMESTAMP)");
        }
        catch (SQLException ex)
        {
            Logger.getLogger(DBInitializer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
