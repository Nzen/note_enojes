/** see ../../../../../LICENSE for release details */
package org.eno_lang.locale;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

/**  */
public class TestLocaleFiles
{
	private static final String rbmAnalysisProperties = "Analysis",
			rbmLoadersList = "org.eno_lang.locale.Loaders";

	/** @param args */
	public static void main( String[] args )
	{
		ResourceBundle byProp = ResourceBundle.getBundle( rbmAnalysisProperties, Locale.GERMAN );
		Enumeration<String> unenhancedFor = byProp.getKeys();
		String key;
		while ( unenhancedFor.hasMoreElements() )
		{
			key = unenhancedFor.nextElement();
			System.out.println( "key "+ key );
			System.out.println( "val\t"+ byProp.getString( key ) );
			
		}

		ResourceBundle byList = ResourceBundle.getBundle( rbmLoadersList, Locale.GERMAN );
		unenhancedFor = byList.getKeys();
		while ( unenhancedFor.hasMoreElements() )
		{
			key = unenhancedFor.nextElement();
			System.out.println( "key "+ key );
			System.out.println( "val\t"+ byList.getString( key ) );
			
		}
	}

}


















