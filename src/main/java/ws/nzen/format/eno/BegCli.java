/** see ../../../../../LICENSE for release details */
package ws.nzen.format.eno;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**  */
public class BegCli
{
	private static final String cl = "bc.";
	static final String configFlagS = "c", configFlagLong = "config", // S is for short
			verboseFlagS = "v", verboseFlagLong = "verbose",
			helpFlagS = "h", helpFlagLong = "help";
	private static final String rbmFile = "bc_messages";
	private static final String rbKeyCliParse = "cliParse",
			rbKeySetNullProp = "nullProperties",
			rbKeyPropDne = "propInitNoFile",
			rbKeyPropIoExc = "propInitIoException",
			rbKeyInpDir = "invalidInputDir";
	private Properties sessionConfig;
	private ResourceBundle rbm;
	private boolean verbose = false; // IMPROVE use or discard

	/** @param args */
	public static void main( String[] args )
	{
		ResourceBundle rbm = ResourceBundle.getBundle( rbmFile );
		CommandLine userInput = prepCli( prepCliParser(), args, rbm );
		BegCli doesStuff = prepDoer( userInput );
		doesStuff.setSessionConfig( prepConfig( userInput, rbm ) );
		doesStuff.satisfySession();
	}


	/** fills options with our cli flags and text */
	public static Options prepCliParser()
	{
		Options knowsCliDtd = new Options();
		final boolean needsEmbelishment = true;
		knowsCliDtd.addOption( configFlagS, configFlagLong, needsEmbelishment,
				"path to config (ex C:\\Program Files\\apache\\tomcat.txt)" );
		knowsCliDtd.addOption( verboseFlagS, verboseFlagLong,
				! needsEmbelishment, "show debug information" );
		knowsCliDtd.addOption( helpFlagS, helpFlagLong,
				! needsEmbelishment, "show arg flags" );
		return knowsCliDtd;
	}


	/** Parses the actual input and shows help, if requested */
	public static CommandLine prepCli( Options knowsCliDtd,
			String[] args, ResourceBundle rbm )
	{
		CommandLineParser cliRegex = new DefaultParser();
		CommandLine userInput = null;
		try
		{
			userInput = cliRegex.parse( knowsCliDtd, args );
			if ( userInput.hasOption( helpFlagS )
					|| userInput.hasOption( helpFlagLong ) )
			{
				new HelpFormatter().printHelp( "Note Enojes", knowsCliDtd );
			}
		}
		catch ( ParseException pe )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbKeyCliParse ) );
			System.err.println( problem.format( new Object[]{ cl +"pc", pe } ) );
		}
		return userInput;
	}


	public static BegCli prepDoer( CommandLine userInput )
	{
		BegCli doesStuff;
		if ( userInput != null && ( userInput.hasOption( verboseFlagS )
				|| userInput.hasOption( verboseFlagLong ) ) )
		 {
			doesStuff = new BegCli( true );
		 }
		else
		{
			 doesStuff = new BegCli();
		}
		return doesStuff;
	}


	public static Properties prepConfig(
			CommandLine userInput, ResourceBundle rbm )
	{
		final String here = cl +"pc";
		Properties resourceMap = new Properties();
		if ( userInput != null )
		{
			String configPath = "";
			if ( userInput.hasOption( configFlagS )
					|| userInput.hasOption( configFlagLong ) )
			{
				configPath = userInput.getOptionValue( configFlagS, "" );
			}
			if ( ! configPath.isEmpty() )
			{
				try ( FileReader ioIn = new FileReader( configPath ) )
				{
					resourceMap.load( ioIn );
				}
				catch ( FileNotFoundException fnfe )
				{
					MessageFormat problem = new MessageFormat( rbm.getString( rbKeyPropDne ) );
					System.err.println( problem.format( new Object[]{ here, configPath, fnfe } ) );
				}
				catch ( IOException ie )
				{
					MessageFormat problem = new MessageFormat( rbm.getString( rbKeyPropIoExc ) );
					System.err.println( problem.format( new Object[]{ here, configPath, ie } )  );
					ie.printStackTrace();
				}
			}
		}
		return resourceMap;
	}


	public BegCli()
	{
		this( false );
	}


	public BegCli( boolean noiseTolerance )
	{
		verbose = noiseTolerance;
	}


	public void satisfySession()
	{
		BundlesEnoGettext beg = new BundlesEnoGettext( sessionConfig );
		Path localeFolder;
		String localePath = "";
		try
		{
			final String currentDir = ( File.pathSeparator.equals( ":" ) )
					? "." : ""; // NOTE windows / linux current dir
			 localePath = sessionConfig.getProperty(
					BundlesEnoGettext.begPropInDir, currentDir );
			localeFolder = Paths.get( localePath );
		}
		catch ( InvalidPathException ipe )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbKeyInpDir ) );
			System.err.println( problem.format( new Object[]{ cl +"ss", localePath, ipe } )  );
			localeFolder = null;
		}
		if ( localeFolder != null )
		{
			beg.bundleDirectory( localeFolder );
		}
		// else, we're done, above already complained
	}


	public Properties getSessionConfig()
	{
		return sessionConfig;
	}
	/** throws npe if sessionConfig is null */
	public void setSessionConfig( Properties sessionConfig )
	{
		if ( sessionConfig == null )
		{
			MessageFormat problem = new MessageFormat( rbm.getString( rbKeySetNullProp ) );
			throw new NullPointerException( problem
					.format( new Object[]{ cl +"ssc" } ) );
		}
		this.sessionConfig = sessionConfig;
	}

}


















