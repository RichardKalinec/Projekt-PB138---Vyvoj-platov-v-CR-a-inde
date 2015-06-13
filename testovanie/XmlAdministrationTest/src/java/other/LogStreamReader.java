/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package other;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author Marek
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
            System.err.println("Error while reading from input stream of external app: " + ex.getMessage());
        }
    }
}
