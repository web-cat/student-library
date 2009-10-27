package student.web;

//-------------------------------------------------------------------------
/**
 *  This interface represents a specialized {@link HtmlElement} that
 *  represents an HTML heading tag--e.g., H1, H2, H3, H4, H5, or H6.
 *  It adds a method to get the heading level as an integer value.
 *
 *  @author  Stephen Edwards
 *  @version 2008.03.29
 */
public interface HtmlHeadingElement
    extends HtmlElement
{
    // ----------------------------------------------------------
    /**
     * Get the heading level (1-6) of this element.
     * @return The heading's level.
     */
    int getHeadingLevel();
}
