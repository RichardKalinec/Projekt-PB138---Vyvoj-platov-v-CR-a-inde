package servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet taking care of login part of web.
 *
 * @author Marek Jonis
 */
@WebServlet(urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet
{
    //private Connection con = null;
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request Servlet request.
     * @param response Servlet response.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request Servlet request.
     * @param response Servlet response.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        /*try
        {
            PreparedStatement st = con.prepareStatement("SELECT * FROM ADMIN WHERE USERNAME=? AND PASSWORD=?");
            st.setString(1, username);
            st.setString(2, password);
            ResultSet rs = st.executeQuery();
            if( (rs != null) && (rs.next()) )
            {
                request.getSession().setAttribute("username", username);
            }*/
        
        //------------
        if(username.equals("jergi") && (password.equals("bakalar")))
        {
            request.getSession().setAttribute("username", username);
        }
        //------------
        
            else
            {
                request.setAttribute("message", "Invalid user or password.");
            }
        /*}
        catch (SQLException ex)
        {
            Logger.getLogger(LoginServlet.class.getName()).log(Level.SEVERE, null, ex);
            request.setAttribute("message", ex.getMessage());
        }*/
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return A String containing servlet description.
     */
    @Override
    public String getServletInfo()
    {
        return "Login servlet";
    }

    /*@Override
    public void init() throws ServletException
    {
        try
        {
            DriverManager.registerDriver(new org.apache.derby.jdbc.ClientDriver());
            con = DriverManager.getConnection("jdbc:derby://localhost:1527/Test", "test", "test");
        }
        catch (SQLException ex)
        {
            Logger.getLogger(LoginServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void destroy()
    {
        if(con != null)
        {
            try
            {
                con.close();
            }
            catch (SQLException ex)
            {
                Logger.getLogger(LoginServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }*/
}
