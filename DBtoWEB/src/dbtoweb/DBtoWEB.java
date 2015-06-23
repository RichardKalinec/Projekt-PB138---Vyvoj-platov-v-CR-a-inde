/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dbtoweb;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.basex.core.*;
import org.basex.core.cmd.*;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 *
 * @author Peter
 */
public class DBtoWEB {

    static Context context = new Context();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws QueryException, IOException{
      
        String[] povolania = new String[]{"povolanie", "informatik", "strojvodca", "instruktor"};
        String[] filtre = new String[] {"vek,20", "pohlavie,muz", "okres,moravsky"};
        GetOneComparision(povolania,filtre);
    }
    
    public static String GetOneComparision(String[] comparision, String[] filters) throws  QueryException, IOException
    {
        double[] values = new double[comparision.length - 1];
        new CreateDB("Salaries", "src/dbtoweb/testovaci.xml").execute(context);
        new Close().execute(context);
        new Open("Salaries").execute(context);
        String xquery;
        for(int i = 1; i < comparision.length; i++)
        {
            double myFinal  = 0;
            int count = 0;
            xquery = "for $salary in /salaries/salary "
                + "where $salary[@" + comparision[0]+"="+"\""+comparision[i]+"\"";
            
            for(int j = 0; j < filters.length; j++)
            {
                String[] twoStrings = filters[j].split(",");
                xquery += " and @" + twoStrings[0] + "=" + "\""+twoStrings[1]+"\"";
            }
            xquery += "] return $salary/text()";
            
            new XQuery(xquery).execute(context);
           
            try(QueryProcessor proc = new QueryProcessor(xquery, context)) {
            // Store the pointer to the result in an iterator:
            Iter iter = proc.iter();

            Pattern pattern = Pattern.compile("\\[#text: (\\d+)\\]");
            for(Item item; (item = iter.next()) != null;) {
                 String value = item.toJava().toString();
                 Matcher matcher = pattern.matcher(value);
                 if(matcher.matches())
                 {
                    double intValue = Double.parseDouble(matcher.group(1));
                    myFinal += intValue;
                    count++;
                 }
                }
            }
            if(count == 0)
            {
                values[i-1] = 0;
            }
            else
            {
                myFinal = myFinal/count;
                values[i-1] = myFinal;
            }
        }
        return GraphImage(values, comparision);
    }
    
    public String GetOneComparision(String[] comparision, String[] comparision2, String[] filters)
    {
        return "halala";
    }
    
    private static String GraphImage(double[] values, String[] names) throws IOException
    {
      int height = 250;
      int wihdt = values.length * 75 + 50;
      BufferedImage bi = new BufferedImage(wihdt, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D ig2 = bi.createGraphics();
      ig2.setPaint(Color.black);
      ig2.drawLine(10, height-20, wihdt-10, height-20);
      ig2.drawLine(10, 10, 10, height-20);
      Font font = new Font("Arial", Font.PLAIN, 10);
      ig2.setFont(font);
      double min = GetMin(values);
      double max = GetMax(values);
      for(int i = 1; i < names.length; i++)
      {
        if(i%2 == 0)
        {
            ig2.drawString(names[i], (i-1)*75+20, height-10);
        }
        else
        {
            ig2.drawString(names[i], (i-1)*75+20, height-5);
        }
        int tempY = (int)(((max-values[i-1])/(max-min))*175)+30;
        ig2.drawRect((i-1)*75+25, tempY, 20, height-20-tempY);
        ig2.drawString(String.format("%.1f", values[i-1]), (i-1)*75+25, tempY-10);
      }
      String path = "d:\\yourImageName.JPG";
      ImageIO.write(bi, "PNG", new File(path));
 
        return path;
    }
    
    private static double GetMin(double[] values)
    {
        double minimum = Double.MAX_VALUE;
        for(int i = 0; i < values.length; i++)
        {
            if(values[i] < minimum)
            {
                minimum = values[i];
            }
        }
        return minimum;
    }
    
    private static double GetMax(double[] values)
    {
        double maximum = Double.MIN_VALUE;
        for(int i = 0; i < values.length; i++)
        {
            if(values[i] > maximum)
            {
                maximum = values[i];
            }
        }
        return maximum;
    }
    
}
