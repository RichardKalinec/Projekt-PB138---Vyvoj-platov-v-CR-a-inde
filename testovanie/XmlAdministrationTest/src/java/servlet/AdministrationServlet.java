package servlet;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import other.UploadedFile;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 *
 * @author Marek
 */
@WebServlet(urlPatterns = {"/administration/*"})
@MultipartConfig
public class AdministrationServlet extends HttpServlet
{
    private static boolean processing = false;
    public static final String UPLOAD_SUBFOLDER = "uploadedFiles";
    private static final SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private SortedSet<UploadedFile> getUploadedFiles(HttpServletRequest request)
    {
        File directory = new File(request.getServletContext().getRealPath("") + File.separator + UPLOAD_SUBFOLDER);
        SortedSet<UploadedFile> files = new TreeSet<>();
        if(directory.exists())
        {
            for(File f : directory.listFiles())
            {
                UploadedFile uploadedFile = new UploadedFile();
                uploadedFile.setFilename(f.getName());
                uploadedFile.setUploaded(formater.format(f.lastModified()));
                files.add(uploadedFile);
            }
        }
        return files;
    }
    
    private String getFileName(Part part)
    {
        for(String content : part.getHeader("content-disposition").split(";"))
        {
            if (content.trim().startsWith("filename"))
            {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    private String getNewFilename(String savePath, String filename)
    {
        String newFilename;
        int pos = filename.lastIndexOf(".");
        boolean cont = true;
        int n = 1;
        
        do
        {
            if(pos == -1)
            {
                newFilename = filename + " (" + n + ")";
            }
            else
            {
                newFilename = filename.substring(0, pos) + " (" + n + ")" + filename.substring(pos);
            }
            
            String s = savePath + File.separator + newFilename;
            if((new File(s)).exists())
            {
                ++n;
            }
            else
            {
                cont = false;
            }
        }while(cont);
        
        return newFilename;
    }
    
    public boolean getProcessing()
    {
        return processing;
    }
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        //PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);
        if(session != null)
        {
            request.setAttribute("username", session.getAttribute("username"));
            request.setAttribute("files", getUploadedFiles(request));
        }
        request.getRequestDispatcher("/administration.jsp").forward(request, response);
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String action = request.getPathInfo();
        switch(action)
        {
            case "/logout":
            {
                HttpSession session = request.getSession();
                session.invalidate();
                response.sendRedirect("/administration");
                break;
            }
            /*case "/delete":
            {
                String filename = URLDecoder.decode(request.getQueryString().substring(9), "UTF-8");
                String directory = request.getServletContext().getRealPath("") + File.separator + UPLOAD_SUBFOLDER;
                File uploadedFile = new File(directory + File.separator + filename);
                uploadedFile.delete();
                request.setAttribute("files", getUploadedFiles(request));
                response.sendRedirect("/administration");
                break;
            }*/
            case "/upload":
            {
                String savePath = request.getServletContext().getRealPath("") + File.separator + UPLOAD_SUBFOLDER;
                File directory = new File(savePath);
                if(!directory.exists())
                {
                    directory.mkdir();
                }                
                Part filePart = request.getPart("file");
                String filename = getFileName(filePart);
                
                if( (filename == null) || (filename.isEmpty()) )
                {
                    request.setAttribute("message", "You have to select file first.");
                    request.setAttribute("files", getUploadedFiles(request));
                    request.getRequestDispatcher("/administration.jsp").forward(request, response);
                }
                else if(filename.endsWith(".txt") || filename.endsWith(".doc"))
                {
                    File file = new File(savePath + File.separator + filename);
                    if(!file.exists())
                    {
                        filePart.write(savePath + File.separator + filename);
                    }
                    else
                    {
                        filePart.write(savePath + File.separator + getNewFilename(savePath, filename));
                    }
                    request.setAttribute("files", getUploadedFiles(request));
                    response.sendRedirect("/administration");
                }
                else
                {
                    request.setAttribute("message", "Wrong file extension.");
                    request.setAttribute("files", getUploadedFiles(request));
                    request.getRequestDispatcher("/administration.jsp").forward(request, response);
                }
                break;
            }
            case "/process":
            {
                String[] files = request.getParameterValues("selectedFiles");
                if(files != null)
                {
                    if(request.getParameter("deleteFiles") != null)
                    {
                        String directory = request.getServletContext().getRealPath("") + File.separator + UPLOAD_SUBFOLDER;
                        for(String filename : files)
                        {
                            File uploadedFile = new File(directory + File.separator + filename);
                            uploadedFile.delete();
                        }
                    }
                    else if(request.getParameter("processFiles") != null)
                    {
                        //
                    }
                    request.setAttribute("files", getUploadedFiles(request));
                    response.sendRedirect("/administration");                    
                }
                else
                {
                    request.setAttribute("message", "You have to select at least one file.");
                    request.setAttribute("files", getUploadedFiles(request));
                    request.getRequestDispatcher("/administration.jsp").forward(request, response);
                }
                break;
            }
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "Short description";
    }
}
