/** see ../../../../../LICENSE for release details */
package ws.nzen.format.eno;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
			rbmBol = "bundleOneLocale",
			rbmDf = "destinationDirectory",
			rbmDsf = "deleteSingularFile",
			rbmEs = "emitSuccess",
			rbmEf = "emitFailure";
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
		final Path destinationFolder = sessionDestinationFolder( dir );
		final PathMatcher onlyPo = dir.getFileSystem().getPathMatcher( "glob:**/*.po" );
		try
		( final Stream<Path> fileChannel = Files.list( dir ) )
		{
			fileChannel.filter( onlyPo::matches ).forEach(
					( poFile ) -> bundleOneLocale( poFile, destinationFolder ) );
		}
		catch ( IOException ie )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmBd ) );
			System.err.println( problem.format( new Object[]{ here, dir, ie } ) );
		}
	}


	public void bundleOneLocale( Path poFile, Path targetFolder )
	{
		final String here = cl +"bol";
		final int wrapperLen = " \"".length();
		String category = "", msgKey= "", msgValue= "";
		Map<String,List<String>> messages = new HashMap<>();
		try
		{
			for ( String line : Files.readAllLines( poFile, Charset.forName( "UTF-8" ) ) )
			{
				if ( line.startsWith( categoryKey ) )
				{
					category = line.substring( line
							.indexOf( "'" ) +1, line.length() -1 ); // NOTE between quotes
					messages.put( category, new LinkedList<>() );
				}
				else if ( line.startsWith( messageKeyKey ) )
				{
					// substring to just that part
					msgKey = line.substring( messageKeyKey
							.length() + wrapperLen, line.length() -1 );
					msgKey = msgKey.replaceAll( "'", "''" );
				}
				else if ( line.startsWith( messageValueKey ) )
				{
					msgValue = line.substring( messageValueKey
							.length() + wrapperLen, line.length() -1 );
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
		emitResourceBundles( messages, poFile, targetFolder );
	}


	private Path filenamefor( Path original, Path targetFolder, String category )
	{
		StringBuilder filename = new StringBuilder( category );
		filename.append( "_" );
		String language = original.getFileName().toString();
		filename.append( language.substring( 0, language.lastIndexOf( '.' ) ) );
		String extension = config.getProperty( begPropStyle, "properties" )
				.toLowerCase().equals( "list" ) ? ".java" : ".properties";
		filename.append( extension );
		Path result = targetFolder.resolve( filename.toString() );
		return result;
	}


	private void emitResourceBundles( Map<String,List<String>> messages,
			Path poFile, Path targetFolder )
	{
		final String here = cl +"erb";
		String category = "messages";
		boolean separateFilePerCategory = config
				.getProperty( begPropSeparate, "false" )
				.toLowerCase().equals( "true" );
		if ( ! separateFilePerCategory )
		{
			Path previousCommonFile = filenamefor(
					poFile, targetFolder, category );
			try
			{
				Files.deleteIfExists( previousCommonFile );
			}
			catch ( IOException ie )
			{
				MessageFormat problem = new MessageFormat( rbm.getString( rbmDsf ) );
				System.err.println( problem.format( new Object[]{
						here, previousCommonFile, ie } ) );
			}
		}
		StringBuilder fileContent = new StringBuilder();
		for ( String currCategory : messages.keySet() )
		{
			fileContent.append( System.lineSeparator() );
			fileContent.append( "# " );
			fileContent.append( currCategory );
			fileContent.append( System.lineSeparator() );
			List<String> pairs = messages.get( currCategory );
			for ( String onePair : pairs )
			{
				fileContent.append( onePair );
				fileContent.append( System.lineSeparator() );
			}
			if ( separateFilePerCategory )
			{
				category = currCategory;
			}
			writeTo( filenamefor( poFile, targetFolder, category ),
					fileContent.toString(), ! separateFilePerCategory );
			fileContent.delete( 0, fileContent.length() );
		}
	}


	private Path sessionDestinationFolder( Path fallback )
	{
		final String here = cl +"sdf";
		final String currentDir = ( File.pathSeparator.equals( ":" ) )
				? "." : ""; // NOTE windows / linux current dir
		try
		{
			String basePath = config.getProperty( begPropOutDir, currentDir );
			if ( config.getProperty( begPropPackageDir, "false" )
					.toLowerCase().equals( "true" )
				&& ! config.getProperty( begPropPackage, "" ).isEmpty() )
			{
				if ( ! basePath.endsWith( File.separator ) )
				{
					basePath += File.separator;
				}
				basePath += config.getProperty( begPropPackage )
						.replaceAll( "\\.", File.separator );
			}
			return Paths.get( basePath );
		}
		catch ( InvalidPathException ipe )
		{
			String folderAttempted = config.getProperty( begPropOutDir, currentDir );
			MessageFormat problem = new MessageFormat( rbm.getString( rbmDf ) );
			System.err.println( problem.format( new Object[]{
					here, folderAttempted, ipe } ) );
			return fallback;
		}
	}


	private void writeTo( Path destination,
			String entireContent, boolean append )
	{
		final String here = cl +"wt";
		OpenOption whetherAppend = ( append )
				? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
		try
		{
			Files.createDirectories( destination.getParent() );
			Files.write( destination, entireContent.getBytes(),
					StandardOpenOption.CREATE, whetherAppend );
			MessageFormat feedback = new MessageFormat( rbm.getString( rbmEs ) );
			System.out.println( feedback.format( new Object[]{ here, destination } ) );
		}
		catch ( IOException ie )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmEf ) );
			System.err.println( problem.format( new Object[]{ here, destination, ie } ) );
		}
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





































