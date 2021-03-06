package admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Runnable class for outputting stream of external application.
 *
 * @author Marek Jonis
 */
public class LogStreamReader implements Runnable
{
    private final BufferedReader reader;
    
    public LogStreamReader(InputStream is)
    {
        reader = new BufferedReader(new InputStreamReader(is));
    }
    
    @Override
    public void run()
    {
        try
        {
            String line = reader.readLine();
            while(line != null)
            {
                System.out.println(line);
                line = reader.readLine();
            }
            reader.close();
        }
        catch(IOException ex)
        {
            System.err.println("Error while reading from stream of external app: " + ex.getMessage());
        }
    }
}
