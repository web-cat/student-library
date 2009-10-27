package student.web;

import java.io.Serializable;
import java.net.*;
import java.util.*;

//-------------------------------------------------------------------------
/**
 *  This interface defines the common features of RSS entities like
 *  {@link RssEntry} objects and {@link RssFeed} objects.
 *
 *  @version 2007.09.01
 *  @author Stephen Edwards
 */
public interface RssEntity
    extends Serializable, Cloneable
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     *  Get this entity's author.  If there are multiple authors,
     *  this method returns the first one.
     *  @return This entity's author
     */
    String getAuthor();


    // ----------------------------------------------------------
    /**
     *  Set this entity's author.
     *  @param author This entity's author
     */
    void setAuthor(String author);


    // ----------------------------------------------------------
    /**
     *  Get this entity's authors.
     *  @return This entity's authors
     */
    List<String> getAuthors();


    // ----------------------------------------------------------
    /**
     *  Set this entity's authors.
     *  @param authors A list of this entity's authors
     */
    void setAuthors(List<String> authors);


    // ----------------------------------------------------------
    /**
     *  Get this entity's publication date.
     *  @return This entity's date
     */
    Date getDate();


    // ----------------------------------------------------------
    /**
     *  Set this entity's publication date.
     *  @param date This entity's date
     */
    void setDate(Date date);


    // ----------------------------------------------------------
    /**
     *  Get this entity's description.
     *  @return This entity's description
     */
    String getDescription();


    // ----------------------------------------------------------
    /**
     *  Set this entity's description.
     *  @param description The new description
     */
    void setDescription(String description);


    // ----------------------------------------------------------
    /**
     *  Get this entity's link as a URL.
     *  @return The link's URL, or null if there is none
     */
    URL getLink();


    // ----------------------------------------------------------
    /**
     *  Set this entity's link.
     *  @param link The new URL to use
     */
    void setLink(URL link);


    // ----------------------------------------------------------
    /**
     *  Get this entity's title.
     *  @return The entity's title
     */
    String getTitle();


    // ----------------------------------------------------------
    /**
     *  Set this entity's title.
     *  @param title The entity's title
     */
    void setTitle(String title);


}
