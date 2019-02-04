
/** see ../../../../../LICENSE for release details */
package ws.nzen.format.eno.localeparse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/** Convert lang.po files from eno-locale to ListResourceBundle
 * or properties files */
public class BundlesEnoResources
{
	private static final String cl = "ber.";
	private static final String ownVersion = "3.0";
	public static final String begPropInDir = "input_path",
			begPropOutDir = "output_directory",
			begPropSeparate = "separate_by_category",
			begPropStyle = "resource_bundle_style",
			begPropPackageDir = "output_into_package",
			begPropPackage = "java_package",
			begPropReplaceVars = "replace_variables",
			begPropListTemplate = "list_template_file",
			begPropIdMap = "create_id_map",
			begPropIdMapTemplate = "id_map_template_file";
	private static final String rbmFile = "ber_messages",
			rbmBd = "bundleDirectory",
			rbmBol = "bundleOneLocale",
			rbmDf = "destinationDirectory",
			rbmDsf = "deleteSingularFile",
			rbmEs = "emitSuccess",
			rbmEf = "emitFailure",
			rbmTdne = "templateDoesNotExist";
	private static final String mjKeyMetaIndex = "meta",
			mjKeyMetaWhen = "timestamp",
			mjKeyMetaVersion = "version",
			mjKeyMsgIndex = "messages";
	private ResourceBundle rbm;
	private Properties config;


	public BundlesEnoResources()
	{
		this( new Properties() );
	}


	public BundlesEnoResources( Properties sessionConfig )
	{
		setConfig( sessionConfig );
		rbm = ResourceBundle.getBundle( rbmFile );
	}


	public void bundleMessages( Path jsonPath )
	{
		final String here = cl +"bd";
		if ( ! jsonPath.getFileName().toString()
				.toLowerCase().endsWith( "json" ) )
		{
			throw new RuntimeException( here +"expecting a json file, not "
					+ jsonPath.getFileName().toString() );
		}
		String entireFile = "";
		try
		{
			entireFile = new String( Files.readAllBytes( jsonPath ) );
		}
		catch ( IOException ie )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmBd ) );
			System.err.println( problem.format( new Object[]{ here, jsonPath, ie } ) );
		}
		// NOTE not recovering from OutOfMemoryError, 2GB is too many messages
		final Path destinationFolder = sessionDestinationFolder( jsonPath.getParent() );
		JSONObject entireCatalog = JSON.parseObject( entireFile );
		String version = versionComment( entireCatalog );
		JSONObject messageCatalog = entireCatalog.getJSONObject( mjKeyMsgIndex );
		for ( String language : messageCatalog.keySet() )
		{
			JSONObject currLang = messageCatalog.getJSONObject( language );
			bundleOneLocale( currLang, destinationFolder,
					language, version );
		}
	}


	/** returns paths of created files */
	public void bundleOneLocale( JSONObject langMessages, Path targetFolder,
			String langAbbreviation, String version )
	{
		String msgKey= "", msgValue= "";
		boolean shouldReplaceVariables = config.getProperty(
				begPropReplaceVars, "false" ).toLowerCase().equals( "true" );
		boolean resourceIsProperties = config.getProperty( begPropStyle, "properties" )
				.toLowerCase().equals( "properties" );
		Map<String, String> keyAlias = new  TreeMap<>(); // NOTE so these are sorted
		Map<String,List<String>> messages = new HashMap<>();
		Map<String, String> keysWithVars = new  HashMap<>();
		int idInd = 0;
		for ( String category : langMessages.keySet() )
		{
			JSONObject currCategory = langMessages.getJSONObject( category );
			List<String> categoryMessages = new ArrayList<>();
			idInd = 0;
			for ( String msgId : currCategory.keySet() )
			{
				msgValue = currCategory.getString( msgId );
				keyAlias.put( category + idInd, msgId );
				if ( resourceIsProperties )
				{
					msgKey = msgId.replaceAll( " ", "\\\\ " ); // NOTE escape spaces
				}
				else
				{
					msgKey = msgId;
				}
				msgValue = msgValue.replaceAll( "'", "''" ); // NOTE single quote x2 
				if ( shouldReplaceVariables )
				{
					msgValue = replaceVarsForMessageFormat( msgValue );
					keysWithVars.put( category + idInd, msgValue );
				}
				if ( resourceIsProperties )
				{
					categoryMessages.add( msgKey +" = "+ msgValue );
				}
				else
				{
					// { "key", "value" },
					categoryMessages.add(
							"{\""+ msgKey +"\", \""+ msgValue +"\"}," );
				}
				idInd++;
			}
			messages.put( category, categoryMessages );
		}
		emitResourceBundles( messages, langAbbreviation,
				targetFolder, resourceIsProperties, version );
		if ( config.getProperty( begPropIdMap, "false" ).equals( "true" ) )
		{
			emitKeyAlias( keyAlias, targetFolder, version, keysWithVars );
		}
	}


	/** replaces known variables (ex [LINE]) with escape flags ready
	 * for use with MessageFormat (ex {0} or {2,number} ) */
	private String replaceVarsForMessageFormat( String msgValue )
	{
		if ( msgValue == null || msgValue.isEmpty() || ! msgValue.startsWith( "(" ) )
		{
			return msgValue;
		}
		StringBuilder changedValue = new StringBuilder( msgValue.length() );
		int indOfReplace = 0, prevInd = 0, variableInd = 0;
		NavigableMap<Integer, EnoLocaleVariables> indType = new TreeMap<>();
		final String endOfLambda = "=> ";
		prevInd = msgValue.indexOf( endOfLambda ) + endOfLambda.length();
			// NOTE skipping lambda "(args) =>"
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


	private void emitResourceBundles( Map<String,List<String>> messages,
			String langAbbreviation, Path targetFolder,
			boolean asProperties, String version )
	{
		final String here = cl +"erb";
		String category = "Messages";
		boolean separateFilePerCategory = config
				.getProperty( begPropSeparate, "false" )
				.toLowerCase().equals( "true" );
		if ( ! separateFilePerCategory )
		{
			Path previousCommonFile = filenamefor( targetFolder,
					category, langAbbreviation, asProperties );
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
			emitAsProperties( messages, targetFolder,
					separateFilePerCategory, category,
					langAbbreviation, asProperties, version );
		}
		else
		{
			emitAsList( messages, targetFolder,
					separateFilePerCategory, category,
					langAbbreviation, asProperties, version );
		}
	}


	private void emitAsProperties( Map<String,List<String>> messages,
			Path targetFolder, boolean separateFiles, String category,
			String language, boolean asProperties, String version )
	{
		StringBuilder fileContent = new StringBuilder();
		if ( separateFiles )
		{
			for ( String currCategory : messages.keySet() )
			{
				fileContent.append( System.lineSeparator() );
				fileContent.append( "# " );
				fileContent.append( version.replace( System.lineSeparator(),
						System.lineSeparator() +"# " ) ); // NOTE ensure multi line version has comment prefix
				fileContent.append( System.lineSeparator() );
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
				writeTo( filenamefor( targetFolder, currCategory,
							language, asProperties ),
						fileContent.toString() );
				fileContent.delete( 0, fileContent.length() -1 );
			}
		}
		else
		{
			fileContent.append( System.lineSeparator() );
			fileContent.append( "# " );
			fileContent.append( version.replace( System.lineSeparator(),
					System.lineSeparator() +"# " ) ); // NOTE ensure multi line version has comment prefix
			fileContent.append( System.lineSeparator() );
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
			}
			writeTo( filenamefor( targetFolder, category,
						language, asProperties ),
					fileContent.toString() );
		}
	}


	private void emitAsList( Map<String,List<String>> messages,
			Path targetFolder, boolean separateFiles, String category,
			String language, boolean asProperties, String version )
	{
		boolean appendWhenWriting = true;
		int minForEachCategory = 50;
		StringBuilder fileContent;
		String lrbUpperHalf = templateForListResourceBundle();
		String lrbLowerHalf = "\t\t\t{\"IMPROVE\",\"b.e.g. should handle better\"}\n\t\t};\n\t}\n}\n\n";
		String filePackage = config.getProperty( begPropPackage, "" );
		filePackage = ( ! filePackage.isEmpty() )
				? "package "+ filePackage +";\n" : filePackage;
		if ( separateFiles )
		{
			for ( String currCategory : messages.keySet() )
			{
				fileContent = new StringBuilder( messages.size() * minForEachCategory );
				fileContent.append( String.format( lrbUpperHalf, filePackage, version,
						classNameFor( currCategory, language ) ) );
				fileContent.append( "\n\t\t\t// " );
				fileContent.append( currCategory );
				fileContent.append( "\n" );
				for ( String pair : messages.get( currCategory ) )
				{
					fileContent.append( "\t\t\t" );
					fileContent.append( pair );
					fileContent.append( System.lineSeparator() );
				}
				fileContent.append( lrbLowerHalf );
				writeTo( filenamefor( targetFolder, currCategory,
							language, asProperties ),
						fileContent.toString() );
			}
		}
		else
		{
			fileContent = new StringBuilder( messages.size() * minForEachCategory );
			lrbUpperHalf = String.format( lrbUpperHalf, filePackage, version,
					classNameFor( category, language ) );
			fileContent.append( lrbUpperHalf );
			for ( String currCategory : messages.keySet() )
			{
				fileContent.append( "\n\t\t\t// " );
				fileContent.append( currCategory );
				fileContent.append( "\n" );
				for ( String pair : messages.get( currCategory ) )
				{
					fileContent.append( "\t\t\t" );
					fileContent.append( pair );
					fileContent.append( System.lineSeparator() );
				}
			}
			fileContent.append( lrbLowerHalf );
			writeTo( filenamefor( targetFolder, category,
						language, asProperties ),
					fileContent.toString() );
		}
	}


	/** assumes similar values to ListResourceBundle */
	private void emitKeyAlias( Map<String, String> keyAlias, Path targetFolder,
			String version, Map<String, String> keysWithVars )
	{
		final String here = cl +"eka ";
		int minForEachCategory = 50;
		StringBuilder fileContent = new StringBuilder( keyAlias.size() * minForEachCategory );
		String aliasTemplate = templateForKeyAlias();
		String filePackage = config.getProperty( begPropPackage, "" );
		filePackage = ( ! filePackage.isEmpty() )
				? "package "+ filePackage +";\n" : filePackage;
		final String varFormat = "[^\\\\{]*" + "(\\{\\d" + "(\\,\\w+)?" + "\\})";
		try
		{
			Pattern fsmSpec = Pattern.compile( varFormat );
			int ind = 0, enoughForDistinct = 3;
			String category = "";
			for ( String alias : keyAlias.keySet() )
			{
				if ( !  alias.substring( 0, enoughForDistinct ).equals( category ) )
				{
					category = alias.substring( 0, enoughForDistinct ); 
					fileContent.append( System.lineSeparator() );
					fileContent.append( "\t// " );
					fileContent.append( alias.substring( 0, alias.length() -1 ) ); // NOTE removing the index 
					fileContent.append( System.lineSeparator() );
					ind = 0;
				}
				String enoId = keyAlias.get( alias );
				fileContent = withRelevantJavadoc( fileContent,
						keysWithVars.get( alias ), fsmSpec );
				fileContent.append( "\tpublic static final String " );
				fileContent.append( enoId.replace( ' ', '_' ).toUpperCase() );
				fileContent.append( " = \"" );
				fileContent.append( enoId );
				fileContent.append( "\";" ); 
				fileContent.append( System.lineSeparator() );
				ind++;
				if ( ind > 5 )
				{
					ind = 0; 
					fileContent.append( System.lineSeparator() );
				}
			}
		}
		catch ( IllegalArgumentException iae )
		{
			System.err.println( here +"couldn't interpret javadoc"
					+" as {0} because "+ iae );
		}
		String className = "EnoAlias";
		writeTo( targetFolder.resolve( className +".java" ),
				String.format( aliasTemplate, filePackage, version,
						className, fileContent.toString() ) );
	}


	private StringBuilder withRelevantJavadoc(StringBuilder fileContent,
			String localizedLineWithVars, Pattern fsmSpec ) throws IllegalArgumentException
	{
		if ( localizedLineWithVars == null || localizedLineWithVars.isEmpty()
				|| ! localizedLineWithVars.contains( "{" ) )
		{
			return fileContent;
		}
		Matcher fsmRuntime = fsmSpec.matcher( localizedLineWithVars );
		final int varInd = 1;
		int cursorInd = 0;
		String temp;
		fileContent.append( "\t/** " );
		while ( fsmRuntime.find( cursorInd ) )
		{
			temp = fsmRuntime.group( varInd );
			if ( ! temp.contains( "," ) )
			{
				temp = temp.substring( 0, temp.length() -1 )
						+ ",string}";
			}
			fileContent.append( temp );
			fileContent.append( " " );
			cursorInd = fsmRuntime.end();
		}
		fileContent.append( " */" );
		fileContent.append( System.lineSeparator() );
		return fileContent;
	}


	private Path filenamefor( Path targetFolder, String category,
			String language, boolean asProperties )
	{
		StringBuilder filename = new StringBuilder( category );
		filename.append( "_" );
		filename.append( language );
		String extension = asProperties ? ".properties" : ".java";
		filename.append( extension );
		Path result = targetFolder.resolve( filename.toString() );
		return result;
	}


	private String classNameFor( String baseName, String language )
	{
		return baseName +"_"+ language;
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


	/** current time; if catalog has time and version, this adds those too */
	private String versionComment( JSONObject messageCatalog )
	{
		StringBuilder comment = new StringBuilder();
		comment.append( "\tGenerated v" );
		comment.append( ownVersion );
		comment.append( " at " );
		comment.append( LocalDateTime.now().toString() );
		if ( messageCatalog != null && messageCatalog.containsKey( mjKeyMetaIndex ) )
		{
			JSONObject metaInfo = messageCatalog.getJSONObject( mjKeyMetaIndex );
			if ( metaInfo.containsKey( mjKeyMetaVersion )
					&& metaInfo.containsKey( mjKeyMetaWhen ) )
			{
				comment.append( System.lineSeparator() );
				comment.append( "\tUsing eno-locale messages.json v" );
				comment.append( metaInfo.getString( mjKeyMetaVersion ) );
				comment.append( " buillt " );
				comment.append( metaInfo.getString( mjKeyMetaWhen ) );
			}
		}
		return comment.toString();
	}


	/** has two Formatter escape marks for package, classname; caller closes three braces  */
	private String templateForListResourceBundle()
	{
		String here = cl +"tflrb";
		String templatePath = config.getProperty(
				begPropListTemplate, "etc/template_lrb_class_upper.txt" );
		Path templateOnClasspath = null;
		try
		{
			templateOnClasspath = Paths.get( templatePath );
		}
		catch ( InvalidPathException ipe )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmTdne ) );
			System.err.println( problem.format(
					new Object[]{ here, templatePath, ipe } ) );
		}
		String templateBody = null;
		if ( templateOnClasspath != null )
		{
			try
			{
				templateBody = new String( Files.readAllBytes( templateOnClasspath ) );
			}
			catch ( IOException ie )
			{
				MessageFormat problem = new MessageFormat( rbm.getString( rbmTdne ) );
				System.err.println( problem.format(
						new Object[]{ here, templatePath, ie } ) );
			}
		}
		if ( templateOnClasspath == null || templateBody == null )
		{
			templateBody = "%1$s\n\timport java.util.ListResourceBundle;\n\tpublic class %2$s extends"
					+ " ListResourceBundle\n{\n\tprotected Object[][] getContents() {\n\treturn new Object[][] {\n";
		}
		return templateBody;
	}


	/** has two Formatter escape marks for package, classname; caller closes three braces  */
	private String templateForKeyAlias()
	{
		String here = cl +"tflrb";
		String templatePath = config.getProperty(
				begPropIdMapTemplate, "etc/template_map_class.txt" );
		Path templateOnClasspath = null;
		try
		{
			templateOnClasspath = Paths.get( templatePath );
		}
		catch ( InvalidPathException ipe )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbmTdne ) );
			System.err.println( problem.format(
					new Object[]{ here, templatePath, ipe } ) );
		}
		String templateBody = null;
		if ( templateOnClasspath != null )
		{
			try
			{
				templateBody = new String( Files.readAllBytes( templateOnClasspath ) );
			}
			catch ( IOException ie )
			{
				MessageFormat problem = new MessageFormat( rbm.getString( rbmTdne ) );
				System.err.println( problem.format(
						new Object[]{ here, templatePath, ie } ) );
			}
		}
		if ( templateOnClasspath == null || templateBody == null )
		{
			templateBody = "%1$s\n\tpublic class %2$s\n{\n%3$s\n}";
		}
		return templateBody;
	}


	private void writeTo( Path destination, String entireContent )
	{
		final String here = cl +"wt";
		OpenOption whetherAppend = StandardOpenOption.TRUNCATE_EXISTING;
		try
		{
			List<String> justOne = new LinkedList<>();
			justOne.add( entireContent );
			Files.createDirectories( destination.getParent() );
			Files.write( destination, justOne, Charset.forName( "UTF-8" ),
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





































