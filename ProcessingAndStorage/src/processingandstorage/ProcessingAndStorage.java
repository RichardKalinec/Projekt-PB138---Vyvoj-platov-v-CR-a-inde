package processingandstorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.basex.core.Context;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Open;

/**
 * Class ProcessingAndStorage processes data about salaries from given TSV
 * files, selects data for a particular information only from newest file
 * of those containing it and updates the Salaries database with this data.
 *
 * @author Richard
 */
public class ProcessingAndStorage {
    /**
     * Logs problems that ocurred when running the code in this class.
     */
    private static final Logger LOG = Logger.getLogger(ProcessingAndStorage.class.getName());
    
    /**
     * Administers loading data from the files specified by arguments and
     * stores the loaded data to the Salaries database.
     * 
     * @param args the command line arguments - paths to TSV files for
     * processing by date uploaded from the newest to the oldest
     * CAUTION! Arguments - paths to files have to be sorted from the newest
     * to the oldest, otherwise outdated data from the former file will be
     * stored in the database, as conflicting data from the latter (even
     * newer) files won't be taken into consideration!
     */
    public static void main(String[] args) throws BaseXException {
        //initiate the database system
        Context context = new Context();
        //open the database with salaries
        new Open("Salaries").execute(context);
        
        try
        {
            for(String arg: args)
            {
                if(arg.startsWith("eurostat"))
                {
                    processAndStoreEurostatSalaries(arg, context);
                }
                else if(arg.startsWith("csu"))
                {

                }
                else
                {
                    String message = "Cannot identify known source of the file, skipping file " + arg + "!";
                    LOG.warning(message);
                }
            }
        }
        catch(IOException ex)
        {
            String msg = "Error reading file!\n" + ex.getMessage();
            LOG.severe(msg);
            return;
        }

        //close the database system
        context.close();
    }
    
    private static void processAndStoreEurostatSalaries(String file, Context context) throws IOException
    {
        //load file content separated into lines
        List<String> dataLines = Files.readAllLines(Paths.get(file));

        //split first line into cells
        String[] dataCells = dataLines.get(0).split("\t");
        
        //separate attributes for H axis and V axis
        String[] vhattributes = dataCells[0].split("\\\\");
        /* check if the time is the only H axis attribute - if not,
         * skip this file as this one is not convenient for
         * processing by this application
         */
        if(!vhattributes[1].equals("time"))
        {
            String message = "Inconvenient file format - time is not the only H axis attribute, skipping " + file + "!";
            LOG.warning(message);
            return;
        }
        //extract attributes from the first cell
        String[] attributes = vhattributes[0].split(",");
        //fetch time labels for columns from the rest of the first line
        List<String> times = new ArrayList<>();
        for(int i = 1; i < dataCells.length; i++)
        {
            times.add(dataCells[i]);
        }

        //prepare temporary variables
        String[] attValues = null;
        String filteredNumericCell = "";
        char character = 0;
        //process data from the file and store it into the database
        for(int lineNumber = 1; lineNumber < dataLines.size(); lineNumber++)
        {
            //split line into cells
            dataCells = dataLines.get(0).split("\t");
            
            
            attValues = dataCells[0].split(",");
            for(int columnNumber = 1; columnNumber < dataCells.length; columnNumber++)
            {
                filteredNumericCell = "";
                for(int i = 0; i < dataCells[columnNumber].length(); i++)
                {
                    character = dataCells[columnNumber].charAt(i);
                    if(Character.isDigit(character) || (character == '.'))
                    {
                        filteredNumericCell += character;
                    }
                }
                if(!filteredNumericCell.isEmpty())
                {
                    
                }
            }
        }
    }
}
