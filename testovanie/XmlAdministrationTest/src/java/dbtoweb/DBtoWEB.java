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

    //private static final Context context = new Context();
        
    public static String GetOneComparision(String dir, String[] comparision, String[] filters, char time) throws  QueryException, IOException
    {
        Context context = new Context();
        double[] values = new double[comparision.length - 1];
        new CreateDB("Salaries", dir + File.separator + "testovaci.xml").execute(context);
        new Close().execute(context);
        new Open("Salaries").execute(context);
        String xquery;
        String filtersAsString = GetFiltersAsString(filters);
        for(int i = 1; i < comparision.length; i++)
        {
            xquery = "for $salary in /salaries/salary "
                + "where $salary[@" + comparision[0]+"="+"\""+comparision[i]+"\""
                + filtersAsString;
            if(time == 'r')
            {
                xquery += " and not(starts-with(@time, 'Q'))";
            }
            else
            {
                xquery += " and starts-with(@time, 'Q')";
            }
            
            xquery += "] return $salary/text()";
            new XQuery(xquery).execute(context);
           
            try(QueryProcessor proc = new QueryProcessor(xquery, context)) {
                values[i-1] = GetValues(proc);
            }
        }
        return GraphImage(dir, values, comparision);
    }
    
    public static String GetOneComparision(String dir, String[] comparision, String[] comparision2, String[] filters, char time) throws  QueryException, IOException
    {
        Context context = new Context();
        double[][] values = new double[comparision.length - 1][comparision2.length-1];
        new CreateDB("Salaries", dir + File.separator + "testovaci.xml").execute(context);
        new Close().execute(context);
        new Open("Salaries").execute(context);
        String xquery;
        String filtersAsString = GetFiltersAsString(filters);
        for(int i = 1; i < comparision.length; i++)
        {
            for(int j = 1;j < comparision2.length; j++)
            {
                xquery = "for $salary in /salaries/salary "
                + "where $salary[@" + comparision[0]+"="+"\""+comparision[i]+"\"";
                xquery += " and @" + comparision2[0]+"="+"\""+comparision2[j]+"\""+ filtersAsString;
                if(time == 'r')
                {
                    xquery += " and not(starts-with(@time, 'Q'))";
                }
                else
                {
                    xquery += " and starts-with(@time, 'Q')";
                }
            
                xquery += "] return $salary/text()";
                new XQuery(xquery).execute(context);
           
                try(QueryProcessor proc = new QueryProcessor(xquery, context)) {
                    values[i-1][j-1] = GetValues(proc);
                }
            }
        }
        return TableImage(dir, values, comparision, comparision2);
    }
    
    private static String GraphImage(String dir, double[] values, String[] names) throws IOException
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
        ig2.drawString(names[i], (i-1)*75+20, height-5);
        int tempY = (int)(((max-values[i-1])/(max-min))*175)+30;
        ig2.drawRect((i-1)*75+25, tempY, 20, height-20-tempY);
        ig2.drawString(String.format("%.1f", values[i-1]), (i-1)*75+25, tempY-10);
      }
      String path = dir + File.separator + "images/graph_"+names[0]+"_Image.JPG";
      ImageIO.write(bi, "PNG", new File(path));
      return path;
    }
    
    private static String TableImage(String dir, double[][] values, String[] comparision, String[] comparision2) throws IOException
    {
        int height = values.length * 25 + 150;
        int width = values[0].length * 100 + 150;
        int rowHeight = (height-20)/(values.length + 1);
        int columnWidth = (width-20)/(values[0].length + 1);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig2 = bi.createGraphics();
        ig2.setPaint(Color.black);
        ig2.drawRect(10, 10, width - 20, height - 20);
        Font font = new Font("Arial", Font.PLAIN, 15);
        ig2.setFont(font);
        for(int i = 0; i <= values.length; i++)
        {
            ig2.drawLine(10, i*rowHeight+10, width-10, i*rowHeight+10);
            for(int j = 0; j <= values[0].length; j++)
            {
                ig2.drawLine(j*columnWidth+10, 10, j*columnWidth+10, height-10);
                if(i==0 && j!=0 )
                {
                    ig2.drawString(comparision2[j], j*columnWidth+20, 40);
                }
                else if(i!=0 && j == 0)
                {
                    ig2.drawString(comparision[i], 20, i*rowHeight+40);
                }
                else if (i!=0 && j!=0)
                {
                    ig2.drawString(values[i-1][j-1]+"", j*columnWidth+20, i*rowHeight+40);
                }
            }
        }
        String path = dir + File.separator + "images/table_"+comparision[0]+"_"+comparision2[0]+"_Image.JPG";
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
    
    private static String GetFiltersAsString(String[] filters)
    {
        String xquery = "";
        for(String filter : filters)
        //for(int j = 0; j < filters.length; j++)
        {
                //String[] twoStrings = filters[j].split(":", 2);
                String[] twoStrings = filter.split(":", 2);
                xquery += " and @" + twoStrings[0] + "=" + "\""+twoStrings[1]+"\"";
        }
        return xquery;
    }
    
    private static double GetValues(QueryProcessor proc) throws QueryException
    {
        int count = 0;
        double sum = 0;
        Iter iter = proc.iter();

            Pattern pattern = Pattern.compile("\\[#text: (\\d+)\\]");
            for(Item item; (item = iter.next()) != null;) {
                 String value = item.toJava().toString();
                 Matcher matcher = pattern.matcher(value);
                 if(matcher.matches())
                 {
                    double intValue = Double.parseDouble(matcher.group(1));
                    sum += intValue;
                    count++;
                 }
                }
            
            if(count == 0)
            {
                sum = 0.0;
            }
            else
            {
                sum = sum/count;
            }
        
        return sum;
    }
}
