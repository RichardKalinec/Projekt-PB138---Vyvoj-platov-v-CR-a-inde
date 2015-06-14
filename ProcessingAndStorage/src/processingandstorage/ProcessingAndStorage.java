package processingandstorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.basex.core.Context;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Close;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.Optimize;
import org.basex.core.cmd.XQuery;

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

        //optimize the database to refresh the index structures
        new Optimize().execute(context);        
        //close the database with salaries
        new Close().execute(context);
        //close the database system
        context.close();
    }
    
    /**
     * Method processAndStoreEurostatSalaries() processes TSV files from
     * Eurostat containing data about salaries with attribute descriptions
     * in the first column of the forst row, attribute values in the first
     * column of the other rows, season (usually a year) in the remaining
     * columns of the forst line, and data about salaries (which are filtered
     * for digits and dots and only cells containing these characters are
     * actually processed) are in the rest of the file. If the record of the
     * salary with given attributes already exists, it is only updated with
     * the new data (which can include inserting data for another seasons), or
     * it is newly created, inserted and filled with the new data, if it
     * doesn't.
     * 
     * @param file A file from Eurostat to process. Data about salaries
     *             formatted as aforementioned is expected.
     * @param context Database context.
     * @throws IOException 
     */
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
        //process data from the file and store it into the database
        for(int lineNumber = 1; lineNumber < dataLines.size(); lineNumber++)
        {
            //split line into cells
            dataCells = dataLines.get(0).split("\t");
            
            //load attribute values from the first cell of the line
            attValues = dataCells[0].split(",");
            //search the rest of the line for numeric values
            for(int columnNumber = 1; columnNumber < dataCells.length; columnNumber++)
            {
                filteredNumericCell = filterStringForDigitsAndDots(dataCells[columnNumber]);
                
                //if a numeric value is found, store it in the database in the appropriate way
                if(!filteredNumericCell.isEmpty())
                {
                    //ckeck if salary record with these attributes is already in the database
                    String isAlreadyPresentQuery = "let $salaries := doc('Salaries')/salaries" +
                        "for $salary in $salaries" +
                        "where $salary/@";
                    //if it isn't, insert it onto the database
                    if(new XQuery()
                    //proceed with processing the rest of the line 
                    for(; columnNumber < dataCells.length; columnNumber++)
                    {
                        new
                    }
                    break;
                }
            }
        }
    }
    
    private static void processAndStoreCSUSalaries(String file, Context context) throws IOException
    {
        
    }
    
    private static String filterStringForDigitsAndDots(String input)
    {
        char character = 0;
        String output = "";
        for(int i = 0; i < input.length(); i++)
        {
            character = input.charAt(i);
            if(Character.isDigit(character) || (character == '.'))
            {
                output += character;
            }
        }
        return output;
    }
}
