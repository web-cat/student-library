package student.web.internal;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import uk.org.catnip.eddie.FeedData;

public class EddieSyndFeed
    implements SyndFeed, Serializable, Cloneable
{
    // ----------------------------------------------------------
    public EddieSyndFeed(FeedData original)
    {
        innerFeed = original;
    }


    // ----------------------------------------------------------
    @Override
    public Object clone()
    {
        return new EddieSyndFeed(null
            // TODO: (FeedData)innerFeed.clone()
            );
    }


    // ----------------------------------------------------------
    public WireFeed createWireFeed()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public WireFeed createWireFeed(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getAuthor()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public List<String> getAuthors()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public List<String> getCategories()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public List<String> getContributors()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getCopyright()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public SyndContent getDescriptionEx()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getEncoding()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public List<SyndEntry> getEntries()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getFeedType()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public Object getForeignMarkup()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public SyndImage getImage()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getLanguage()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getLink()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public List<String> getLinks()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public Module getModule(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public List<Module> getModules()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public Date getPublishedDate()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public List<String> getSupportedFeedTypes()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getTitle()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public SyndContent getTitleEx()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public String getUri()
    {
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------------------------------------------------
    public void setAuthor(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void setAuthors(List arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void setCategories(List arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void setContributors(List arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setCopyright(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setDescription(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setDescriptionEx(SyndContent arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setEncoding(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void setEntries(List arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setFeedType(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setForeignMarkup(Object arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setImage(SyndImage arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setLanguage(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setLink(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void setLinks(List arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void setModules(List arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setPublishedDate(Date arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setTitle(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setTitleEx(SyndContent arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void setUri(String arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public void copyFrom(Object arg0)
    {
        // TODO Auto-generated method stub

    }


    // ----------------------------------------------------------
    public Class<?> getInterface()
    {
        // TODO Auto-generated method stub
        return null;
    }


    //~ Instance/static variables .............................................

    private FeedData innerFeed;

    private static final long serialVersionUID = -3966083283645021482L;
}
