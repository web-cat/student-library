package student.web;

import student.web.internal.ApplicationSupportStrategy;
import student.web.internal.LocalityService;

/**
 * This class is used to access some support methods that are only activated
 * when the application is running in a server enviornment. This means when
 * these methods are used in local development, no changes to the application
 * will occur.
 * 
 * @author mjw87
 * 
 */
public abstract class WebPageUtil {
	private static ApplicationSupportStrategy support = LocalityService
			.getSupportStrategy();

	private WebPageUtil() {

	}

	// ----------------------------------------------------------
	/**
	 * Cause the web application to show a different web page in the user's web
	 * browser.
	 * 
	 * @param url
	 *            The new web page to show in the user's browser
	 */
	public static void showWebPage(String url) {
		support.showWebPage(url);
	}

	// ----------------------------------------------------------
	/**
	 * Retrieve the name of the current ZHTML file, such as "index.zhtml" or
	 * "lab02.zhtml".
	 * 
	 * @return The name of the current ZHTML file, without any directory
	 *         component, or "" if there is none.
	 */
	public static String getCurrentPageName() {
		return support.getCurrentPageName();
	}

	// ----------------------------------------------------------
	/**
	 * Retrieve the relative path name of the current ZHTML file, such as
	 * "/Fall09/mypid/index.zhtml" or "/Fall09/mypid/lab02/lab02.zhtml".
	 * 
	 * @return The name path to the current ZHTML file, or "" if there is none.
	 */
	public static String getCurrentPagePath() {
		return support.getCurrentPagePath();
	}

	// ----------------------------------------------------------
	/**
	 * Get a parameter passed to this page in the query part of the URL.
	 * 
	 * @param name
	 *            The name of the parameter to retrieve
	 * @return The parameter's value on the current page, or null if there is
	 *         none.
	 */
	public static String getPageParameter(String name) {
		return support.getPageParameter(name);
	}
}
