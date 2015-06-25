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
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;

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
            //process data source files
            for(String arg: args)
            {
                /* file is identified as originating from Eurostat and will be
                 * processed according to the standard layout of files from
                 * Eurostat with the ability to process any attributes
                 * specifying the salaries
                 */
                if(arg.startsWith("eurostat"))
                {
                    processAndStoreEurostatSalaries(arg, context);
                }
                /* file is identified as originating from CSO and will be
                 * processed according to the fixed content and layout category
                 * further specified in the filename, only files with content
                 * and layout compatible with one of these fixed categories and
                 * specified accordingly in their filenames will be processed
                 * correctly
                 */
                else if(arg.startsWith("cso"))
                {
                    processAndStoreCSOSalaries(arg, context);
                }
                /* if none of the known sources is identified, log the error
                 * message and skip processing the file
                 */
                else
                {
                    String message = "Cannot identify known source of the file, skipping file " + arg + "!";
                    LOG.warning(message);
                }
            }
        }
        catch(IOException ex1)
        {
            String msg = "Error reading file!\n" + ex1.getMessage();
            LOG.severe(msg);
            return;
        }
        catch(QueryException ex2)
        {
            String msg = "Error reading file!\n" + ex2.getMessage();
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
     * in the first column of the first row, attribute values in the first
     * column of the other rows, season (usually a year) in the remaining
     * columns of the first line, and data about salaries (which are filtered
     * for digits and dots and only cells containing these characters are
     * actually processed) are in the rest of the file. If the record of the
     * salary with given attributes already exists, it is only updated with
     * the new data, or it is newly created, filled with the new data and
     * inserted, if it doesn't.
     * 
     * @param file A file from Eurostat to process. Data about salaries
     *             formatted as aforementioned is expected.
     * @param context Database context.
     * @throws IOException 
     */
    private static void processAndStoreEurostatSalaries(String file, Context context) throws IOException, QueryException
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
        String filteredNumericCell = null;
        
        //prepare generic inserting or updating query
        String query = "let $salaries := doc('salaries.xml')/salaries\n";
        
        String qVariableDeclarations = "declare variable $salary as xs:string external;\n";
        for(int attNo = 0; attNo < attributes.length; attNo++)
        {
            qVariableDeclarations += "declare variable $attn" + attNo + " as xs:string external;\n";
            qVariableDeclarations += "declare variable $attv" + attNo + " as xs:string external;\n";
        }
        qVariableDeclarations += "declare variable $timev as xs:string external;\n";
        
        String qUpdatingFunctionDeclaration = "declare updating function insert-or-update-salary("
                + "$psalaries as element(salaries), $psalary as xs:string, ";
        for(int attNo = 0; attNo < attributes.length; attNo++)
        {
            qUpdatingFunctionDeclaration += "$pattn" + attNo + " as xs:string, ";
            qUpdatingFunctionDeclaration += "$pattv" + attNo + " as xs:string, ";
        }
        qUpdatingFunctionDeclaration += "$ptimev as xs:string)\n{\n" +
            "let $target-salary := $salaries/salary[@source='EUROSTAT' and ";
        for(int attNo = 0; attNo < attributes.length; attNo++)
        {
            qUpdatingFunctionDeclaration += "@$pattn" + attNo + "='$pattv" + attNo + "' and ";
        }
        qUpdatingFunctionDeclaration += "@time='$ptimev']\n" +
            "if(empty($target-salary))\nthen insert node <salary source=\"EUROSTAT\" ";
        for(int attNo = 0; attNo < attributes.length; attNo++)
        {
            qUpdatingFunctionDeclaration += "$pattn" + attNo + "=\"$pattv" + attNo + "\" ";
        }
        qUpdatingFunctionDeclaration += "time=\"$ptimev\">$psalary</salary> as last into $psalaries\n";
        qUpdatingFunctionDeclaration += "else replace value of node $target-salary with $psalary\n};\n";
        
        String qUpdatingFunctionCall = "insert-or-update-salary($salaries, $salary, ";
        for(int attNo = 0; attNo < attributes.length; attNo++)
        {
            qUpdatingFunctionCall += "$attn" + attNo + ", ";
            qUpdatingFunctionCall += "$attv" + attNo + ", ";
        }
        qUpdatingFunctionCall += "$timev)";
        
        query += qVariableDeclarations + qUpdatingFunctionDeclaration + qUpdatingFunctionCall;
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
                    try(QueryProcessor qp = new QueryProcessor(query, context))
                    {
                        qp.bind("salary", filteredNumericCell);
                        for(int attNo = 0; attNo < attributes.length; attNo++)
                        {
                            qp.bind("attn" + attNo, attributes[attNo]);
                            qp.bind("attv" + attNo, attValues[attNo]);
                        }
                        qp.bind("timev", times.get(columnNumber - 1));

                        qp.value();
                    }
                }
            }
        }
    }
    
    /**
     * Method processAndStoreCSOSalaries() processes TSV files from CSO
     * containing data about salaries that fit into one of 4 recognized
     * categories: quartoen, cznace, reggend and regkazam. Each of them is
     * breifly explained above respective methods for processing them. If the
     * record of the salary with given attributes already exists, it is only
     * updated with the new data, or it is newly created, filled with the new
     * data and inserted, if it doesn't.
     * 
     * @param file A file from CSO to process. Data about salaries formatted
     *             as aforementioned is expected.
     * @param context
     * @throws IOException
     * @throws QueryException 
     */
    
    private static void processAndStoreCSOSalaries(String file, Context context) throws IOException, QueryException
    {
        /* file is identified as containing quarterly data about salaries in
         * Czech Republic - overall, business sector and non-business sector -
         * everything in the known layout
         */
        if(file.startsWith("quartoen", 4))
        {
            processAndStoreCSUquartoen(file, context);
        }
        /* file is identified as containing annual data about salaries in Czech
         * Republic for one year for various occupations classified using
         * CZ-NACE system - everything in the known layout
         */
        else if(file.startsWith("cznace", 4))
        {
            processAndStoreCSUcznace(file, context);
        }
        /* file is identified as containing annual data for one year for all
         * regions of Czech Republic for genders - everything in the known
         * layout
         */
        else if(file.startsWith("reggend", 4))
        {
            processAndStoreCSUreggend(file, context);
        }
        /* file is identified as containing annual data for one year for all
         * regions of Czech Republic for various occupations classified using
         * the main KZAM classes - everything in the known layout
        */
        else if(file.startsWith("regkzam", 4))
        {
            processAndStoreCSUregkzam(file, context);
        }
        /* if none of the known categories is identified, log the error message
         * and skip processing the file
         */
        else
        {
            String message = "Cannot identify known content and layout category of the file from CSO, skipping file " + file + "!";
            LOG.warning(message);
        }
    }
    
    /**
     * Method processAndStoreCSUquartoen() prcesses files from CSO containing
     * quarterly data about salaries in Czech Republic - overall, business
     * sector and non-business sector - everything in the known layout
     * 
     * @param file Filename of the file to process.
     * @param context Database context.
     * @throws IOException
     * @throws QueryException 
     */
    private static void processAndStoreCSUquartoen(String file, Context context) throws IOException, QueryException
    {
        //load file content separated into lines
        List<String> dataLines = Files.readAllLines(Paths.get(file));

        //validate some key features of the file layout
        String[] dataCells = dataLines.get(2).split("\t");
        if(!dataCells[3].equals("Území") || !dataCells[4].equals("Česká republika"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        dataCells = dataLines.get(3).split("\t");
        if(!dataCells[3].equals("Měřicí jednotka"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        //load currency of the salaries
        String currency = dataCells[4];
        //continue validating
        dataCells = dataLines.get(5).split("\t");
        if(!dataCells[0].equals("Období"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        dataCells = dataLines.get(6).split("\t");
        if(!dataCells[2].equals("celkem") || !dataCells[3].equals("podnikatelská sféra") ||
            !dataCells[4].equals("nepodnikatelská sféra"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }

        //prepare temporary variables
        String year = null;
        String time = null;
        String salary = null;
        
        //prepare generic inserting or updating query
        String query = "let $salaries := doc('salaries.xml')/salaries\n";
        
        String qVariableDeclarations = "declare variable $salary as xs:string external;\n"
            + "declare variable $currency as xs:string external;\ndeclare variable $sector as xs:string external;\n"
            + "declare variable $time as xs:string external;\n";
        
        String qUpdatingFunctionDeclaration = "declare updating function insert-or-update-salary("
            + "$psalaries as element(salaries), $psalary as xs:string, $pcurrency as xs:string, "
            + "$psector as xs:string, $ptime as xs:string)\n{\n"
            + "let $target-salary := $salaries/salary[@source='CSO' and @geo='CZ' and @estruct='GRS' and "
            + "@currency='$pcurrency' and @sector='$psector' and @time='$ptime']\n"
            + "if(empty($target-salary))\nthen insert node <salary source=\"CSO\" geo=\"CZ\" estruct=\"GRS\""
            + "currency=\"$pcurrency\" sector=\"$psector\" time=\"$ptime\">$psalary</salary> as last into $psalaries\n"
            + "else replace value of node $target-salary with $psalary\n};\n";
        
        String qUpdatingFunctionCall = "insert-or-update-salary($salaries, $salary, $currency, $sector, $time)";
        
        query += qVariableDeclarations + qUpdatingFunctionDeclaration + qUpdatingFunctionCall;
        
        /* process quarterly data about salaries for every year and store it into the database,
         * continuously arranged groups of four lines for every year expected
         */
        for(int yearLine = 7; yearLine < dataLines.size(); yearLine += 4)
        {
            //load a year from the first column of the first line of the annual data about salaries
            dataCells = dataLines.get(yearLine).split("\t");
            year = filterStringForDigitsAndCommas(dataCells[0]);
            //if no numerical value is found, assume that this is the end of data and finish processing
            if(year.equals(""))
            {
                break;
            }

            /* process quarterly data about salaries, chronological and continuous arrangement
             * of data for every quarter expected
             */
            for(int quarterLine = yearLine; quarterLine < yearLine + 4; quarterLine++)
            {
                dataCells = dataLines.get(quarterLine).split("\t");
                time = "Q" + ((quarterLine - 6) % 4) + " " + year;
                
                //load overall salary and store it into the database
                salary = dataCells[2];
                try(QueryProcessor qp = new QueryProcessor(query, context))
                {
                    qp.bind("salary", salary);
                    qp.bind("currency", currency);
                    qp.bind("sector", "OVERALL");
                    qp.bind("time", time);
                    
                    qp.value();
                }
                
                //load salary for business sector and store it into the database
                salary = dataCells[2];
                try(QueryProcessor qp = new QueryProcessor(query, context))
                {
                    qp.bind("salary", salary);
                    qp.bind("currency", currency);
                    qp.bind("sector", "BUSINESS");
                    qp.bind("time", time);
                    
                    qp.value();
                }
                
                //load salary for non-business sector and store it into the database
                salary = dataCells[2];
                try(QueryProcessor qp = new QueryProcessor(query, context))
                {
                    qp.bind("salary", salary);
                    qp.bind("currency", currency);
                    qp.bind("sector", "NON-BUSINESS");
                    qp.bind("time", time);
                    
                    qp.value();
                }
            }
        }
    }
    
    /**
     * Method processAndStoreCSUcznace() prcesses files from CSO containing
     * annual data about salaries in Czech Republic for one year for various
     * occupations classified using CZ-NACE system - everything in the known
     * layout
     * 
     * @param file Filename of the file to process.
     * @param context Database context.
     * @throws IOException
     * @throws QueryException 
     */
    private static void processAndStoreCSUcznace(String file, Context context) throws IOException, QueryException
    {
        //load file content separated into lines
        List<String> dataLines = Files.readAllLines(Paths.get(file));

        //validate some key features of the file layout
        String[] dataCells = dataLines.get(2).split("\t");
        if(!dataCells[8].equals("Období"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        //load year the data covers
        String year = filterStringForDigitsAndCommas(dataCells[9]);
        //continue validating
        dataCells = dataLines.get(3).split("\t");
        if(!dataCells[8].equals("Území") || !dataCells[9].equals("Česká republika"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }

        dataCells = dataLines.get(5).split("\t");
        if(!dataCells[4].equals("Průměrná hrubá měsíční mzda (na přepočtené počty zaměstnanců)") ||
            !dataCells[8].equals("Průměrná hrubá měsíční mzda (na fyzické osoby)"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        dataCells = dataLines.get(6).split("\t");
        if(!dataCells[4].equals("v Kč") || !dataCells[8].equals("v Kč"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        dataCells = dataLines.get(7).split("\t");
        if(!dataCells[0].equals("Odvětví celkem"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        
        //prepare generic inserting or updating query
        String query = "let $salaries := doc('salaries.xml')/salaries\n";
        
        String qVariableDeclarations = "declare variable $salary as xs:string external;\n"
            + "declare variable $cz-nace as xs:string external;\n"
            + "declare variable $calculation-employees as xs:string external;\n"
            + "declare variable $time as xs:string external;\n";
        
        String qUpdatingFunctionDeclaration = "declare updating function insert-or-update-salary("
            + "$psalaries as element(salaries), $psalary as xs:string, "
            + "$pcz-nace as xs:string, $pcalculation-employees as xs:string, $ptime as xs:string)\n{\n"
            + "let $target-salary := $salaries/salary[@source='CSO' and @geo='CZ' and @estruct='GRS' and "
            + "@cz_nace='$pcz-nace' and @calculation_employees='$pcalculation-employees' and @time='$ptime']\n"
            + "if(empty($target-salary))\nthen insert node <salary source=\"CSO\" geo=\"CZ\" estruct=\"GRS\""
            + "cz_nace=\"$pcz-nace\" calculation_employees=\"$pcalculation-employees\" time=\"$ptime\">"
            + "$psalary</salary> as last into $psalaries\n"
            + "else replace value of node $target-salary with $psalary\n};\n";
        
        String qUpdatingFunctionCall = "insert-or-update-salary($salaries, $salary, $cz-nace, "
            + "$calculation-employees, $time)";
        
        query += qVariableDeclarations + qUpdatingFunctionDeclaration + qUpdatingFunctionCall;

        /* load overall sectors salaries with the attribute cz_nace set to OVERALL
         * to prevent conflict with general data
         */
        //load salary for recounted employee count and store it into the database
        String salary = dataCells[4];
        salary = filterStringForDigitsAndCommas(dataCells[4]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("cz-nace", "OVERALL");
            qp.bind("calculation-employees", "PER_RECOUNTED_EMPLOYEE_COUNT");
            qp.bind("time", year);

            qp.value();
        }
        //load salary for phisical persons and store it into the database
        salary = dataCells[8];
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("cz-nace", "OVERALL");
            qp.bind("calculation-employees", "PER_RECOUNTED_EMPLOYEE_COUNT");
            qp.bind("time", year);

            qp.value();
        }
        //prepare temporary variables
        String cz_nace = null;
        
        //process data obout salaries for single CZ-NACE occupation groups
        for(int sectorLine = 8; sectorLine < dataLines.size(); sectorLine++)
        {
            dataCells = dataLines.get(sectorLine).split("\t");
            //load sector name
            cz_nace = dataCells[1];
            
            //load salary for recounted employee count and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[4]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("cz-nace", cz_nace);
                qp.bind("calculation-employees", "PER_RECOUNTED_EMPLOYEE_COUNT");
                qp.bind("time", year);
                
                qp.value();
            }
            //load salary for phisical persons and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[8]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("cz-nace", cz_nace);
                qp.bind("calculation-employees", "PER_NATURAL_PERSONS");
                qp.bind("time", year);
                
                qp.value();
            }
        }
    }
    
    /**
     * Method processAndStoreCSUreggend() prcesses files from CSO containing
     * annual data for one year for all regions of Czech Republic for genders -
     * everything in the known layout
     * 
     * @param file Filename of the file to process.
     * @param context Database context.
     * @throws IOException
     * @throws QueryException 
     */
    private static void processAndStoreCSUreggend(String file, Context context) throws IOException, QueryException
    {
        //load file content separated into lines
        List<String> dataLines = Files.readAllLines(Paths.get(file));

        //validate some key features of the file layout
        String[] dataCells = dataLines.get(2).split("\t");
        if(!dataCells[5].equals("Období"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        //load year the data covers
        String year = filterStringForDigitsAndCommas(dataCells[6]);
        //continue validating
        dataCells = dataLines.get(3).split("\t");
        if(!dataCells[5].equals("Měřicí jednotka"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        //load currency of the salaries
        String currency = dataCells[6];
        //continue validating
        dataCells = dataLines.get(5).split("\t");
        if(!dataCells[1].equals("Hrubá měsíční mzda celkem") || !dataCells[4].equals("Medián hrubých měsíčních mezd"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        dataCells = dataLines.get(6).split("\t");
        if(!dataCells[1].equals("celkem") || !dataCells[2].equals("muži") || !dataCells[3].equals("ženy")
            || !dataCells[4].equals("celkem") || !dataCells[5].equals("muži") || !dataCells[6].equals("ženy"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        dataCells = dataLines.get(7).split("\t");
        if(!dataCells[0].equals("Česká republika"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        
        //prepare generic inserting or updating query
        String query = "let $salaries := doc('salaries.xml')/salaries\n";
        
        String qVariableDeclarations = "declare variable $salary as xs:string external;\n"
            + "declare variable $currency as xs:string external;\ndeclare variable $ecase as xs:string external;\n"
            + "declare variable $region as xs:string external;\ndeclare variable $sex as xs:string external;\n"
            + "declare variable $time as xs:string external;\n";
        
        String qUpdatingFunctionDeclaration = "declare updating function insert-or-update-salary("
            + "$psalaries as element(salaries), $psalary as xs:string, $pcurrency as xs:string, "
            + "$pregion as xs:string, $psex as xs:string, $pecase as xs:string, $ptime as xs:string)\n{\n"
            + "let $target-salary := $salaries/salary[@source='CSO' and @geo='CZ' and  @estruct='GRS' and "
            + "@currency='$pcurrency' and @ecase='$pecase' and @region='$pregion' and @sex='$psex' and @time='$ptime']\n"
            + "if(empty($target-salary))\nthen insert node <salary source=\"CSO\" geo=\"CZ\" estruct=\"GRS\""
            + "currency=\"$pcurrency\" ecase=\"$pecase\" region=\"$pregion\" sex=\"$psex\" time=\"$ptime\">"
            + "$psalary</salary> as last into $psalaries\n"
            + "else replace value of node $target-salary with $psalary\n};\n";
        
        String qUpdatingFunctionCall = "insert-or-update-salary($salaries, $salary, $currency, $sector, $time)";
        
        query += qVariableDeclarations + qUpdatingFunctionDeclaration + qUpdatingFunctionCall;

        /* load overall salaries in the Czech republic and store them into the database
         * with the attribute sector set to overall to prevent conflict with general data
         */
        //load overall gross salary for both men and women and store it into the database
        String salary = filterStringForDigitsAndCommas(dataCells[1]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("currency", currency);
            qp.bind("ecase", "OVERALL");
            qp.bind("region", "OVERALL");
            qp.bind("sex", "OVERALL");
            qp.bind("time", year);

            qp.value();
        }
        //load overall gross salary for men and store it into the database
        salary = filterStringForDigitsAndCommas(dataCells[2]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("currency", currency);
            qp.bind("ecase", "OVERALL");
            qp.bind("region", "OVERALL");
            qp.bind("sex", "M");
            qp.bind("time", year);

            qp.value();
        }
        //load overall gross salary for women and store it into the database
        salary = filterStringForDigitsAndCommas(dataCells[3]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("currency", currency);
            qp.bind("ecase", "OVERALL");
            qp.bind("region", "OVERALL");
            qp.bind("sex", "F");
            qp.bind("time", year);

            qp.value();
        }
        //load median gross salary for both men and women and store it into the database
        salary = filterStringForDigitsAndCommas(dataCells[4]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("currency", currency);
            qp.bind("ecase", "MEDIAN");
            qp.bind("region", "OVERALL");
            qp.bind("sex", "OVERALL");
            qp.bind("time", year);

            qp.value();
        }
        //load median gross salary for men and store it into the database
        salary = filterStringForDigitsAndCommas(dataCells[5]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("currency", currency);
            qp.bind("ecase", "MEDIAN");
            qp.bind("region", "OVERALL");
            qp.bind("sex", "M");
            qp.bind("time", year);

            qp.value();
        }
        //load median gross salary for women and store it into the database
        salary = filterStringForDigitsAndCommas(dataCells[6]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("currency", currency);
            qp.bind("ecase", "MEDIAN");
            qp.bind("region", "OVERALL");
            qp.bind("sex", "F");
            qp.bind("time", year);

            qp.value();
        }

        //prepare temporary variables
        String region = null;
        //process data about salaries for single regions
        for(int regionLine = 8; regionLine < dataLines.size(); regionLine++)
        {
            dataCells = dataLines.get(regionLine).split("\t");
            //load region name
            region = filterStringForDigitsAndCommas(dataCells[0]);

            //load overall gross salary for both men and women and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[1]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("ecase", "OVERALL");
                qp.bind("region", region);
                qp.bind("sex", "OVERALL");
                qp.bind("time", year);

                qp.value();
            }
            //load overall gross salary for men and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[2]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("ecase", "OVERALL");
                qp.bind("region", region);
                qp.bind("sex", "M");
                qp.bind("time", year);

                qp.value();
            }
            //load overall gross salary for women and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[3]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("ecase", "OVERALL");
                qp.bind("region", region);
                qp.bind("sex", "F");
                qp.bind("time", year);

                qp.value();
            }
            //load median gross salary for both men and women and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[4]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("ecase", "MEDIAN");
                qp.bind("region", region);
                qp.bind("sex", "OVERALL");
                qp.bind("time", year);

                qp.value();
            }
            //load median gross salary for men and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[5]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("ecase", "MEDIAN");
                qp.bind("region", region);
                qp.bind("sex", "M");
                qp.bind("time", year);

                qp.value();
            }
            //load median gross salary for women and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[6]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("ecase", "MEDIAN");
                qp.bind("region", region);
                qp.bind("sex", "F");
                qp.bind("time", year);

                qp.value();
            }
        }
    }
    
    /**
     * Method processAndStoreCSUregkzam() prcesses files from CSO containing
     * annual data for one year for all regions of Czech Republic for various
     * occupations classified using the main KZAM classes - everything in the
     * known layout
     * 
     * @param file Filename of the file to process.
     * @param context Database context.
     * @throws IOException
     * @throws QueryException 
     */
    private static void processAndStoreCSUregkzam(String file, Context context) throws IOException, QueryException
    {
        //load file content separated into lines
        List<String> dataLines = Files.readAllLines(Paths.get(file));

        //validate some key features of the file layout
        String[] dataCells = dataLines.get(2).split("\t");
        if(!dataCells[9].equals("Období"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        //load year the data covers
        String year = filterStringForDigitsAndCommas(dataCells[10]);
        //continue validating
        dataCells = dataLines.get(3).split("\t");
        if(!dataCells[9].equals("Měřicí jednotka"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        //load currency of the salaries
        String currency = dataCells[10];
        //continue validating
        dataCells = dataLines.get(5).split("\t");
        if(!dataCells[1].equals("Hrubá měsíční mzda celkem") ||
            !dataCells[2].equals("z toho mzda pracovníků podle hlavních tříd KZAM"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        //process the line with KZAM classes (the first two cells will be always empty)
        String[] lineWithKzamCells = dataLines.get(6).split("\t");
        //continue validating
        dataCells = dataLines.get(7).split("\t");
        if(!dataCells[0].equals("Česká republika"))
        {
            String message = "Invalid file layout, skipping file " + file + "!";
            LOG.warning(message);
            return;
        }
        
        //prepare generic inserting or updating query
        String query = "let $salaries := doc('salaries.xml')/salaries\n";
        
        String qVariableDeclarations = "declare variable $salary as xs:string external;\n"
            + "declare variable $currency as xs:string external;\ndeclare variable $region as xs:string external;\n"
            + "declare variable $main-kzam-class as xs:string external;\ndeclare variable $time as xs:string external;\n";
        
        String qUpdatingFunctionDeclaration = "declare updating function insert-or-update-salary("
            + "$psalaries as element(salaries), $psalary as xs:string, $pcurrency as xs:string, "
            + "$pregion as xs:string, $pmain-kzam-class as xs:string,$ptime as xs:string)\n{\n"
            + "let $target-salary := $salaries/salary[@source='CSO' and @geo='CZ' and  @estruct='GRS' and "
            + "@currency='$pcurrency' and @region='$pregion' and @main_kzam_class='$pmain-kzam-class' and @time='$ptime']\n"
            + "if(empty($target-salary))\nthen insert node <salary source=\"CSO\" geo=\"CZ\" estruct=\"GRS\""
            + "currency=\"$pcurrency\" region=\"$pregion\" main_kzam_class=\"$pmain-kzam-class\" time=\"$ptime\">"
            + "$psalary</salary> as last into $psalaries\n"
            + "else replace value of node $target-salary with $psalary\n};\n";
        
        String qUpdatingFunctionCall = "insert-or-update-salary($salaries, $salary, $currency, $sector, $time)";
        
        query += qVariableDeclarations + qUpdatingFunctionDeclaration + qUpdatingFunctionCall;
        
        //prepare temporary variables
        String region = null;
        int kzamClassColumn = 2;
        //load overall salary for Czech republic and store it into the database
        String salary = filterStringForDigitsAndCommas(dataCells[1]);
        try(QueryProcessor qp = new QueryProcessor(query, context))
        {
            qp.bind("salary", salary);
            qp.bind("currency", currency);
            qp.bind("region", "OVERALL");
            qp.bind("main-kzam-class", "OVERALL");
            qp.bind("time", year);

            qp.value();
        }
        //load salries for single KZAM classes for Czech republic and store them into the database
        for(; kzamClassColumn < dataCells.length; kzamClassColumn++)
        {
            salary = filterStringForDigitsAndCommas(dataCells[kzamClassColumn]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("region", "OVERALL");
                qp.bind("main-kzam-class", lineWithKzamCells[kzamClassColumn]);
                qp.bind("time", year);

                qp.value();
            }
        }
        
        //process data about salaries for single regions
        for(int regionLine = 8; regionLine < dataLines.size(); regionLine++)
        {
            dataCells = dataLines.get(regionLine).split("\t");
            //load region name
            region = filterStringForDigitsAndCommas(dataCells[0]);

            //load overall salary for the region and store it into the database
            salary = filterStringForDigitsAndCommas(dataCells[1]);
            try(QueryProcessor qp = new QueryProcessor(query, context))
            {
                qp.bind("salary", salary);
                qp.bind("currency", currency);
                qp.bind("region", region);
                qp.bind("main-kzam-class", "OVERALL");
                qp.bind("time", year);

                qp.value();
            }

            //load salaries for single KZAM classes for Czech republic and store them into the database
            for(kzamClassColumn = 2; kzamClassColumn < dataCells.length; kzamClassColumn++)
            {
                salary = filterStringForDigitsAndCommas(dataCells[kzamClassColumn]);
                try(QueryProcessor qp = new QueryProcessor(query, context))
                {
                    qp.bind("salary", salary);
                    qp.bind("currency", currency);
                    qp.bind("region", region);
                    qp.bind("main-kzam-class", lineWithKzamCells[kzamClassColumn]);
                    qp.bind("time", year);

                    qp.value();
                }
            }
        }
    }
    
    /**
     * Method filterStringForDigitsAndDots() filters the given string for digits
     * and dots - if no digits or dots were in the original string, empty string
     * is returned.
     * 
     * @param input Input string to filter.
     * @return Filtered string.
     */
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
    
    /**
     * Method filterStringForDigitsAndCommas() filters the given string for
     * digits and commas - if no digits or dots were in the original string,
     * empty string is returned.
     * 
     * @param input Input string to filter.
     * @return Filtered string.
     */
    private static String filterStringForDigitsAndCommas(String input)
    {
        char character = 0;
        String output = "";
        for(int i = 0; i < input.length(); i++)
        {
            character = input.charAt(i);
            if(Character.isDigit(character) || (character == ','))
            {
                output += character;
            }
        }
        return output;
    }
}
