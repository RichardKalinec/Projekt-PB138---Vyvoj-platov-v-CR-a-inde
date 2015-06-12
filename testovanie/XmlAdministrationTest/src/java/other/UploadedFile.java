package other;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Marek
 */
public class UploadedFile implements Comparable<UploadedFile>
{
    private String filename;
    private String uploaded;

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public String getUploaded()
    {
        return uploaded;
    }

    public void setUploaded(String uploaded)
    {
        this.uploaded = uploaded;
    }

    @Override
    public int compareTo(UploadedFile o)
    {
        return (-1 * uploaded.compareTo(o.uploaded));
    }
}
