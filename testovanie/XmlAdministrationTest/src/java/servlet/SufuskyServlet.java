package servlet;

import dbtoweb.DBtoWEB;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.basex.query.QueryException;

/**
 *
 * @author Jergi
 */
@WebServlet(name = "SufuskyServlet", urlPatterns = {"/compare"})
public class SufuskyServlet extends HttpServlet
{
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.getRequestDispatcher("/zidan.jsp").forward(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String[] comparision = request.getParameter("comparision").split(",");
        String[] comparision2 = request.getParameter("comparision2").split(",");
        String[] filter = request.getParameter("filter").split(",");
        String time = request.getParameter("time");
        String path = "";
        if(comparision.length > 1)
        {
            try
            {
                if(comparision2.length > 1)
                {
                    path = DBtoWEB.GetComparision(request.getServletContext().getRealPath(""), comparision, comparision2, filter, time.charAt(0));
                }
                else
                {
                    path = DBtoWEB.GetComparision(request.getServletContext().getRealPath(""), comparision, filter, time.charAt(0));
                }
            }
            catch(QueryException ex)
            {
                System.err.println(ex.getMessage());
            }
        }
        request.setAttribute("picture", path);//File.separator + "app" + File.separator + "klub_22424960.jpg");
        request.getRequestDispatcher("/zidan.jsp").forward(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

}
