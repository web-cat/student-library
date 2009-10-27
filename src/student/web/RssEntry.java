package student.web;

import com.sun.syndication.feed.synd.*;
import java.io.Serializable;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import static net.sf.webcat.SystemIOUtilities.isOnServer;

//-------------------------------------------------------------------------
/**
 *  This class represents one entry in a syndication feed, like an
 *  {@link RssFeed}.
 *
 *  @version 2007.09.01
 *  @author Stephen Edwards
 */
public class RssEntry
    implements RssEntity, Serializable, Cloneable
{
    //~ Instance/static variables .............................................

    private static final long serialVersionUID = 7270393242016940052L;

    /** The raw ROME feed entry. */
    protected SyndEntry nativeEntry;

    /** The entry's link URL (set lazily in {@link #getLink()}. */
    protected URL entryLink;

    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new RssEntry object that is completely blank.
     */
    public RssEntry()
    {
        this(new SyndEntryImpl());
    }


    // ----------------------------------------------------------
    /**
     * Creates a new RssEntry object as a copy of an existing entry.
     * Subclasses should override this constructor if they add their
     * own data, so that their internal subclass data gets initialized
     * properly.
     * @param existingEntry The entry to copy
     */
    public RssEntry(RssEntry existingEntry)
    {
        this(existingEntry.nativeEntry);
    }


    // ----------------------------------------------------------
    /**
     * Creates a new RssEntry object from an existing ROME entry.
     * @param entry The ROME entry to wrap
     */
    RssEntry(SyndEntry entry)
    {
        nativeEntry = entry;
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     *  Get this entry's author.  If there are multiple authors,
     *  this method returns the first one.
     *  @return This entry's author
     */
    public String getAuthor()
    {
        return nativeEntry.getAuthor();
    }


    // ----------------------------------------------------------
    /**
     *  Set this entry's author.
     *  @param author This entry's author
     */
    public void setAuthor(String author)
    {
        nativeEntry.setAuthor(author);
    }


    // ----------------------------------------------------------
    /**
     *  Get this entry's authors.
     *  @return This entry's authors
     */
    @SuppressWarnings("unchecked")
    public List<String> getAuthors()
    {
        return nativeEntry.getAuthors();
    }


    // ----------------------------------------------------------
    /**
     *  Set this entry's authors.
     *  @param authors A list of this entry's authors
     */
    public void setAuthors(List<String> authors)
    {
        nativeEntry.setAuthors(authors);
    }


    // ----------------------------------------------------------
    /**
     *  Get this entry's publication date.
     *  @return This entry's date
     */
    public Date getDate()
    {
        Date date = nativeEntry.getUpdatedDate();
        if (date == null)
        {
            date = nativeEntry.getPublishedDate();
        }
        return date;
    }


    // ----------------------------------------------------------
    /**
     *  Set this entry's publication date.
     *  @param date This entry's date
     */
    public void setDate(Date date)
    {
        nativeEntry.setPublishedDate(date);
        nativeEntry.setUpdatedDate(null);
    }


    // ----------------------------------------------------------
    /**
     *  Get this entry's description.
     *  @return This entry's description
     */
    public String getDescription()
    {
        SyndContent description = nativeEntry.getDescription();
        return description == null
            ? null
            : description.getValue();
    }


    // ----------------------------------------------------------
    /**
     *  Set this entry's description.
     *  @param description The new description
     */
    public void setDescription(String description)
    {
        SyndContent desc = nativeEntry.getDescription();
        if (desc == null)
        {
            desc = new SyndContentImpl();
            desc.setType("text/plain");
            nativeEntry.setDescription(desc);
        }
        desc.setValue(description);
    }


    // ----------------------------------------------------------
    /**
     * Get the MIME type of the description (like "text/html" or
     * "text/plain").
     * @return The description's MIME type
     */
    public String getDescriptionType()
    {
        SyndContent description = nativeEntry.getDescription();
        return description == null
            ? "text/plain"
            : description.getType();
    }


    // ----------------------------------------------------------
    /**
     *  Set the MIME type for this entry's description.  The default
     *  (if unset) is "text/plain".
     *  @param mimeType The new MIME type
     */
    public void setDescriptionType(String mimeType)
    {
        SyndContent desc = nativeEntry.getDescription();
        if (desc == null)
        {
            desc = new SyndContentImpl();
            nativeEntry.setDescription(desc);
        }
        desc.setType(mimeType);
    }


    // ----------------------------------------------------------
    /**
     *  Get this entry's link as a URL.
     *  @return The link's URL, or null if there is none
     */
    public URL getLink()
    {
        if (entryLink == null)
        {
            String link = nativeEntry.getLink();
            if (link != null)
            {
                try
                {
                    entryLink = new URL(link);
                }
                catch (MalformedURLException e)
                {
                    if (!isOnServer())
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return entryLink;
    }


    // ----------------------------------------------------------
    /**
     *  Set this entry's link.
     *  @param link The new URL to use
     */
    public void setLink(URL link)
    {
        entryLink = link;
        nativeEntry.setLink(link == null ? null : link.toString());
    }


    // ----------------------------------------------------------
    /**
     *  Get this entry's title.
     *  @return The entry's title
     */
    public String getTitle()
    {
        return nativeEntry.getTitle();
    }


    // ----------------------------------------------------------
    /**
     *  Set this entry's title.
     *  @param title The entry's title
     */
    public void setTitle(String title)
    {
        nativeEntry.setTitle(title);
    }


    // ----------------------------------------------------------
    /**
     *  Clone this object.
     *  @return a copy of this object
     */
    public Object clone()
    {
        try
        {
            return new RssEntry((SyndEntry)nativeEntry.clone());
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException(e);
        }
    }


    // ----------------------------------------------------------
    /**
     *  Generate a printable version of this entry.
     *  @return the string representation of this object
     */
    public String toString()
    {
        return nativeEntry.toString();
    }
}
