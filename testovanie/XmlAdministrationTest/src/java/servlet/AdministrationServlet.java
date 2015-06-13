package servlet;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import other.UploadedFile;
import java.io.IOException;
import java.io.InputStreamReader;
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
import other.LogStreamReader;

/**
 *
 * @author Marek
 */
@WebServlet(urlPatterns = {"/administration/*"})
@MultipartConfig
public class AdministrationServlet extends HttpServlet
{
    private static Process process = null;
    public static final String UPLOAD_SUBFOLDER = "uploadedFiles";
    public static final String EXTERNAL_APP = "app" + File.separator + "TestovaciaAplikacia.jar";
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
            if(content.trim().startsWith("filename"))
            {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    private String getNewFilename(String savePath, String filename)
    {
        String newFilename;
        boolean cont = true;
        int n = 1;
        
        do
        {
            int pos = filename.lastIndexOf(".");
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
    
    private void uploadFile(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
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
        else if(filename.endsWith(".TSV") || filename.endsWith(".tsv"))
        {
            String fileSource = request.getParameter("fileSource");
            if(fileSource.equals("none"))
            {
                request.setAttribute("message", "You have to choose file source.");
                request.setAttribute("files", getUploadedFiles(request));
                request.getRequestDispatcher("/administration.jsp").forward(request, response);
            }
            else
            {
                filename = fileSource + "-" + filename;
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
        }
        else
        {
            request.setAttribute("message", "Wrong file extension.");
            request.setAttribute("files", getUploadedFiles(request));
            request.getRequestDispatcher("/administration.jsp").forward(request, response);
        }
    }
    
    private void deleteFiles(HttpServletRequest request, HttpServletResponse response, String[] filenames) throws IOException
    {
        String directory = request.getServletContext().getRealPath("") + File.separator + UPLOAD_SUBFOLDER;
        for(String filename : filenames)
        {
            File file = new File(directory + File.separator + filename);
            file.delete();
        }
        request.setAttribute("files", getUploadedFiles(request));
        response.sendRedirect("/administration");
    }
    
    private void processFiles(HttpServletRequest request, HttpServletResponse response, String[] filenames) throws IOException, ServletException
    {
        if(!isProcessRunning())
        {
            String[] cmdarray = new String[3 + filenames.length];
            cmdarray[0] = "java";//System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
            cmdarray[1] = "-jar";
            cmdarray[2] = request.getServletContext().getRealPath("") + File.separator + EXTERNAL_APP;
            int i = 3;
            String directory = request.getServletContext().getRealPath("") + File.separator + UPLOAD_SUBFOLDER;
            for(String filename : filenames)
            {
                cmdarray[i] = directory + File.separator + filename;
                ++i;
            }

            process = Runtime.getRuntime().exec(cmdarray);
            LogStreamReader lsr = new LogStreamReader(process.getInputStream());
            Thread t = new Thread(lsr, "LogStreamReader");
            t.start();
            request.setAttribute("files", getUploadedFiles(request));
            response.sendRedirect("/administration");
        }
        else
        {
            request.setAttribute("message", "Processing app is already running.");
            request.setAttribute("files", getUploadedFiles(request));
            request.getRequestDispatcher("/administration.jsp").forward(request, response);
        }
    }
    
    public static boolean isProcessRunning()
    {
        if(process == null)
        {
            return false;
        }
        
        try
        {
            process.exitValue();
            return false;
        }
        catch(IllegalThreadStateException ex)
        {
            return true;
        }
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
            case "/upload":
            {
                uploadFile(request, response);
                break;
            }
            case "/process":
            {
                String[] filenames = request.getParameterValues("selectedFiles");
                if(filenames != null)
                {
                    if(request.getParameter("deleteFiles") != null)
                    {
                        deleteFiles(request, response, filenames);
                    }
                    else if(request.getParameter("processFiles") != null)
                    {
                        processFiles(request, response, filenames);
                    }
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
