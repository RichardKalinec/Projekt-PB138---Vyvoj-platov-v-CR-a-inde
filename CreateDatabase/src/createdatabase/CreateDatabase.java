package createdatabase;

import org.basex.core.*;
import org.basex.core.cmd.*;

/**
 * Class CreateDatabase creates a new database named "Salaries". It uses BaseX
 * as a native XML database and a XML file containing only root tag "salaries".
 * If the database already exists, it is overwritten - therefore all data from
 * the database is cleared.
 *
 * @author Richard Kalinec
 */
public class CreateDatabase {

    /**
     * Runs all the code of the class directly (see class description).
     * @param args (ignored) the command line arguments
     * @throws BaseXException if a database command fails
     */
    public static void main(String[] args) throws BaseXException {
        Context context = new Context();
        
        new CreateDB("Salaries", "src/salariesInit.xml").execute(context);
    }
    
}
