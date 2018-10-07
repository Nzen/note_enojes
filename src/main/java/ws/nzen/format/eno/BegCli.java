/** see ../../../../../LICENSE for release details */
package ws.nzen.format.eno;

import java.nio.file.Path;
import java.nio.file.Paths;

/**  */
public class BegCli
{

	/** @param args */
	public static void main( String[] args )
	{
		/*
		eventually more cli stuff
		*/
		BundlesEnoGettext beg = new BundlesEnoGettext();
		Path enoFolder = Paths.get( "etc" );
		beg.bundleDirectory( enoFolder );
	}

}


















