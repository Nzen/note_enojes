
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
import java.util.NavigableMap;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;
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
			begPropPackage = "java_package",
			begPropReplaceVars = "replace_variables";
			// begPropIdMap = "create_id_map"; 
	private static final String rbmFile = "beg_messages",
			rbmBd = "bundleDirectory",
			rbmBol = "bundleOneLocale",
			rbmDf = "destinationDirectory",
			rbmDsf = "deleteSingularFile",
			rbmEs = "emitSuccess",
			rbmEf = "emitFailure",
			rbmTdne = "templateDoesNotExist";
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
		boolean shouldReplaceVariables = config.getProperty(
				begPropReplaceVars, "false" ).toLowerCase().equals( "true" );
		boolean resourceIsProperties = config.getProperty( begPropStyle, "properties" )
				.toLowerCase().equals( "properties" );
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
					if ( shouldReplaceVariables )
					{
						msgValue = replaceVarsForMessageFormat( msgValue );
					}
					if ( resourceIsProperties )
					{
						messages.get( category ).add( msgKey +" = "+ msgValue );
					}
					else
					{
						// { "key", "value" },
						messages.get( category ).add(
								"{\""+ msgKey +"\", \""+ msgValue +"\"}," );
					}
				}
				// else, skip, it's an unrelated comment or blank
			}
		}
		catch ( IOException ie )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmBol ) );
			System.err.println( problem.format( new Object[]{ here, poFile, ie } ) );
		}
		emitResourceBundles( messages, poFile, targetFolder, resourceIsProperties );
	}


	/** replaces known variables (ex [LINE]) with escape flags ready
	 * for use with MessageFormat. */
	private String replaceVarsForMessageFormat( String msgValue )
	{
		if ( msgValue == null || msgValue.isEmpty() || ! msgValue.contains( "[" ) )
		{
			return msgValue;
		}
		final int likelyIncrease = 20;
		StringBuilder changedValue = new StringBuilder(
				msgValue.length() + likelyIncrease );
		int indOfReplace = 0, prevInd = 0, variableInd = 0;
		NavigableMap<Integer, EnoLocaleVariables> indType = new TreeMap<>();
		while ( true )
		{
			indType.clear();
			for ( EnoLocaleVariables var : EnoLocaleVariables.values() )
			{
				indOfReplace = msgValue.indexOf( var.getTextForm(), prevInd );
				if ( indOfReplace >= 0 )
				{
					indType.put( indOfReplace, var );
				}
			}
			if ( indType.isEmpty() )
			{
				changedValue.append( msgValue.substring( prevInd ) );
				break;
			}
			else
			{
				indOfReplace = indType.firstKey();
				EnoLocaleVariables currVar = indType.get( indOfReplace );
				changedValue.append( msgValue.substring( prevInd, indOfReplace ) );
				changedValue.append( "{" );
				changedValue.append( Integer.toString( variableInd ) );
				if ( currVar.hasTypeInfo() )
				{
					changedValue.append( currVar.getSupplement() );	
				}
				changedValue.append( "}" );
				variableInd++;
				prevInd = indOfReplace + currVar.getTextForm().length();
			}
		}
		return changedValue.toString();
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
			Path poFile, Path targetFolder, boolean asProperties )
	{
		final String here = cl +"erb";
		String category = "Messages";
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
		if ( asProperties )
		{
			emitAsProperties( messages, poFile, targetFolder,
					separateFilePerCategory, category );
		}
		else
		{
			emitAsList( messages, poFile, targetFolder,
					separateFilePerCategory, category );
		}
	}


	private void emitAsProperties( Map<String,List<String>> messages,
			Path poFile, Path targetFolder,
			boolean separateFiles, String category )
	{
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
			if ( separateFiles )
			{
				category = currCategory;
			}
			writeTo( filenamefor( poFile, targetFolder, category ),
					fileContent.toString(), ! separateFiles );
			fileContent.delete( 0, fileContent.length() );
		}
	}


	private void emitAsList( Map<String,List<String>> messages,
			Path poFile, Path targetFolder,
			boolean separateFiles, String category )
	{
		String lrbUpperHalf = templateForListResourceBundle();
		String lrbLowerHalf = "{\"IMPROVE\",\"b.e.g. should handle better\"}\t\t};\n\t}\n}";
		StringBuilder fileContent = new StringBuilder();
		//for ( int ind )
		
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


	private String templateForListResourceBundle()
	{
		String here = cl +"tflrb";
		String templatePath = "template_lrb_class_upper.txt";
		Path templateOnClasspath = null;
		try
		{
			templateOnClasspath = Paths.get( templatePath );
		}
		catch ( InvalidPathException ipe )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmBol ) );
			System.err.println( problem.format(
					new Object[]{ here, templatePath, ipe } ) );
		}
		String templateBody = null;
		//templateBody = Files.
		throw new RuntimeException( "not yet implemented" );
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





































