/*
 * Install.java - Main class of the installer
 *
 * Originally written by Slava Pestov for the jEdit installer project. This work
 * has been placed into the public domain. You may use this work in any way and
 * for any purpose you wish.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package installer;

import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.*;
import java.util.Properties;

public class Install
{
	public static void main(String[] args)
	{
		String javaVersion = System.getProperty("java.version");
		if(javaVersion.compareTo("1.3") < 0)
		{
			System.err.println("You are running Java version "
				+ javaVersion + ".");
			System.err.println("This installer requires Java 1.3 or later.");
			System.exit(1);
		}

		if(args.length == 0)
		{
			MetalLookAndFeel.setCurrentTheme(new JEditMetalTheme());
			new SwingInstall();
		}
		else if(args.length == 1 && args[0].equals("text"))
			new ConsoleInstall();
		else if(args.length >= 2 && args[0].equals("auto"))
		{
			new NonInteractiveInstall(args);
		}
		else
		{
			System.err.println("Usage:");
			System.err.println("java -jar <installer JAR>");
			System.err.println("java -jar <installer JAR> text");
			System.err.println("java -jar <installer JAR> auto"
				+ " <install dir> [unix-script=<dir>] [unix-man=<dir>]");
			System.err.println("text parameter starts installer in text-only mode.");
			System.err.println("auto parameter starts installer in non-interactive mode.");
		}
	}

	public Install()
	{
		props = new Properties();
		try
		{
			InputStream in = getClass().getResourceAsStream("/installer/install.props");
			props.load(in);
			in.close();
		}
		catch(IOException io)
		{
			System.err.println("Error loading 'install.props':");
			io.printStackTrace();
		}

		buf = new byte[32768];
	}

	public String getProperty(String name)
	{
		return props.getProperty(name);
	}

	public int getIntegerProperty(String name)
	{
		try
		{
			return Integer.parseInt(props.getProperty(name));
		}
		catch(Exception e)
		{
			return -1;
		}
	}

	public void copy(InputStream in, String outfile, Progress progress)
		throws IOException
	{
		File outFile = new File(outfile);

		OperatingSystem.getOperatingSystem().mkdirs(outFile.getParent());

		BufferedOutputStream out = new BufferedOutputStream(
			new FileOutputStream(outFile));

		int count;

		for(;;)
		{
			count = in.read(buf,0,Math.min(in.available(),buf.length));
			if(count == -1 || count == 0)
				break;

			out.write(buf,0,count);
			if(progress != null)
				progress.advance(count);
		}

		//in.close();
		out.close();
	}

	// private members
	private Properties props;
	private byte[] buf;
}
