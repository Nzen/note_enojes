/** see ../../../../../LICENSE for release details */
package ws.nzen.format.eno;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/** Convert lang.po files from eno-locale to ListResourceBundle java files */
public class BundlesEnoGettext
{
	private static final String cl = "beg.";
	private static final String rbmFile = "beg_messages",
			rbmBd = "bundleDirectory",
			rbmBol = "bundleOneLocale";
	private static final String categoryKey = "# Message group ",
			messageKeyKey = "msgid", messageValueKey = "msgstr";
	private ResourceBundle rbm;


	public BundlesEnoGettext()
	{
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
		String className, msgKey;
		Map<String,String> messages = new HashMap<>();
		try
		{
			for ( String line : Files.readAllLines( poFile, Charset.forName( "UTF-8" ) ) )
			{
				if ( line.startsWith( categoryKey ) )
				{
					System.out.println( line );
				}
				else if ( line.startsWith( messageKeyKey ) )
				{
					// substring to just that part
					//msgKey = 
				}
				else if ( line.startsWith( messageValueKey ) )
				{
					System.out.println( line );
				}
				// else, skip, it's a comment or blank
			}
		}
		catch ( IOException ie )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmBol ) );
			System.err.println( problem.format( new Object[]{ here, poFile, ie } ) );
		}
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


}





































