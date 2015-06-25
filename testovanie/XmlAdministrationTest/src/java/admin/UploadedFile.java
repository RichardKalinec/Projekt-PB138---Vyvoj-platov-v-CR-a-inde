package admin;

/**
 * Provides storing of information about uploaded file.
 *
 * @author Marek Jonis
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
    public int compareTo(UploadedFile other)
    {
        if(uploaded.length() > other.uploaded.length())
        {
            return 1;
        }
        if(uploaded.length() < other.uploaded.length())
        {
            return -1;
        }
        return uploaded.compareTo(other.uploaded);
    }
}
