/** see ../../../../../LICENSE for release details */
package ws.nzen.format.eno;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/** Convert lang.po files from eno-locale to ListResourceBundle java files */
public class BundlesEnoGettext
{
	private static final String cl = "beg.";
	public static final String begPropInDir = "input_directory",
			begPropOutDir = "output_directory",
			begPropSeparate = "separate_by_category",
			begPropStyle = "resource_bundle_style",
			begPropPackageDir = "output_into_package",
			begPropPackage = "package";
			// begPropIdMap = "create_id_map"; 
	private static final String rbmFile = "beg_messages",
			rbmBd = "bundleDirectory",
			rbmBol = "bundleOneLocale";
	private static final String categoryKey = "# Message group ",
			messageKeyKey = "msgid", messageValueKey = "msgstr";
	private ResourceBundle rbm;
	private Properties config;


	public BundlesEnoGettext()
	{
		this( new Properties() );
	}


	public BundlesEnoGettext( Properties sessionConfig )
	{
		setConfig( sessionConfig );
		rbm = ResourceBundle.getBundle( rbmFile );
	}


	public void bundleDirectory( Path dir )
	{
		final String here = cl +"bd";
		final PathMatcher onlyPo = dir.getFileSystem().getPathMatcher( "glob:**/*.po" );
		try
		( final Stream<Path> fileChannel = Files.list( dir ) )
		{
			fileChannel.filter( onlyPo::matches ).forEach( ( poFile ) -> bundleOneLocale( poFile ) );
		}
		catch ( IOException ie )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmBd ) );
			System.err.println( problem.format( new Object[]{ here, dir, ie } ) );
		}
	}


	public void bundleOneLocale( Path poFile )
	{
		final String here = cl +"bol";
		final int wrapperLen = " \"".length(), trailingQuote;
		String className= "", category = "", msgKey= "", msgValue= "";
		Map<String,List<String>> messages = new HashMap<>();
		try
		{
			for ( String line : Files.readAllLines( poFile, Charset.forName( "UTF-8" ) ) )
			{
				if ( line.startsWith( categoryKey ) )
				{
					category = line; // NOTE currently using because it has a comment prefix
					messages.put( category, new LinkedList<>() );
				}
				else if ( line.startsWith( messageKeyKey ) )
				{
					// substring to just that part
					msgKey = line.substring( messageKeyKey.length() + wrapperLen, line.length() -1 );
					msgKey = msgKey.replaceAll( "'", "''" );
				}
				else if ( line.startsWith( messageValueKey ) )
				{
					msgValue = line.substring( messageValueKey.length() + wrapperLen, line.length() -1 );
					msgValue = msgValue.replaceAll( "'", "''" );
					// IMPROVE handle variables, ex [FIELDSET_NAME]
					messages.get( category ).add( msgKey +" = "+ msgValue );
				}
				// else, skip, it's an unrelated comment or blank
			}
		}
		catch ( IOException ie )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmBol ) );
			System.err.println( problem.format( new Object[]{ here, poFile, ie } ) );
		}
		StringBuilder fileContent = new StringBuilder();
		for ( String currCategory : messages.keySet() )
		{
			fileContent.append( System.lineSeparator() );
			fileContent.append( currCategory );
			fileContent.append( System.lineSeparator() );
			List<String> pairs = messages.get( currCategory );
			for ( String onePair : pairs )
			{
				fileContent.append( onePair );
				fileContent.append( System.lineSeparator() );
			}
			// if config.separate files write separately
		}
		writeTo( poFile, fileContent.toString() );// FIX shortcut, gen actual name later
	}
	/*
(or config that tells me more stuff, ugh, this can spin out but I just need the simple bit firstI

get folder from cli or assume it's the current folder
for file in folder that filename ends with po,
 open file
 for line in file,
  if line starts with 'message group' comment, write current map to file(s), start new current map
  else if line starts with msgid, start new message
  else if line starts with msgstr, end the current message, put in current map
	 */

	private void writeTo( Path destination, String entireContent )
	{
		System.out.println( "\t"+ destination );
		System.out.println( entireContent );
	}


	public Properties getConfig()
	{
		return config;
	}
	public void setConfig( Properties config )
	{
		this.config = config;
	}


}





































