package pl.pwr.hiervis;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.core.LoadedHierarchy;
import pl.pwr.hiervis.ui.VisualizerFrame;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.Utils;


public final class HierarchyVisualizer
{
	private static final Logger log = LogManager.getLogger( HierarchyVisualizer.class );

	public static final String APP_NAME = "Hierarchy Visualizer";

	private static Options options;


	private HierarchyVisualizer()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	public static void main( String[] args ) throws Exception
	{
		createOptions();

		CommandLine cmd = parseArgs( args );

		if ( cmd.hasOption( 'h' ) || cmd.hasOption( "help" ) ) {
			printHelp();
			System.exit( 0 );
		}

		String subtitle = null;
		if ( cmd.hasOption( 's' ) ) {
			subtitle = cmd.getOptionValue( 's' );
		}

		configureLoggers( subtitle == null ? "hv" : subtitle );

		// Check if the program can access its own folder
		if ( new File( "." ).exists() == false ) {
			log.error( "Failed to access current working directory." );
			SwingUIUtils.showErrorDialog(
				"Program was unable to access the current working directory.\n\n" +
					"Make sure that you're not trying to run the program from inside of a zip archive.\n" +
					"Also, instead of double-clicking on hv.jar, try to launch start.bat/start.sh"
			);

			System.exit( 0 );
		}

		HVContext context = new HVContext();
		context.setConfig( loadConfig() );

		File inputFile = null;
		LoadedHierarchy.Options loadOptions = null;
		if ( cmd.hasOption( 'i' ) ) {
			List<String> inputOptions = new ArrayList<String>( Arrays.asList( cmd.getOptionValues( 'i' ) ) );
			boolean withTrueClass = inputOptions.remove( "true-class" );
			boolean withInstanceNames = inputOptions.remove( "instance-names" );
			boolean withHeader = inputOptions.remove( "header" );
			boolean fixBreadthGaps = inputOptions.remove( "fix-breadth-gaps" );
			boolean useSubtree = inputOptions.remove( "use-subtree" );

			inputFile = new File( inputOptions.get( 0 ) );

			loadOptions = new LoadedHierarchy.Options(
				withInstanceNames,
				withTrueClass,
				withHeader,
				fixBreadthGaps,
				useSubtree
			);

			if ( inputFile.isDirectory() ) {
				throw new IOException( inputFile.getPath() + " must be a path to a file!" );
			}
			if ( !inputFile.exists() ) {
				throw new FileNotFoundException( inputFile.getPath() );
			}
		}

		try {
			context.getMeasureManager().loadMeasureFiles( Paths.get( "scripts/measures" ) );
		}
		catch ( IOException e ) {
			log.error( "IO exception while loading measure files: ", e );
		}
		catch ( Throwable e ) {
			log.error( "Unexpected error occurred while loading measure files: ", e );
		}

		executeGUI( context, subtitle, inputFile, loadOptions );
	}

	@SuppressWarnings("static-access")
	private static void createOptions()
	{
		Option inputOpt = OptionBuilder
			.withArgName( "file path" )
			.hasOptionalArgs( 4 )
			.hasArgs( 5 )
			.isRequired( false )
			.withDescription( "path to a *.csv file describing a hierarchy to load on app start" )
			.create( "i" );

		Option subtitleOpt = OptionBuilder
			.withArgName( "name" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription( "optional subtitle for frame titles, to help identify them" )
			.create( "s" );

		Option helpOpt = OptionBuilder
			.hasArg( false )
			.isRequired( false )
			.withDescription( "prints this message" )
			.withLongOpt( "help" )
			.create( 'h' );

		options = new Options();
		options.addOption( helpOpt );
		options.addOption( inputOpt );
		options.addOption( subtitleOpt );
	}

	private static void printHelp()
	{
		new HelpFormatter().printHelp( "Hierarchy Visualizer", options );
	}

	private static CommandLine parseArgs( String[] args ) throws ParseException
	{
		try {
			return new BasicParser().parse( options, args );
		}
		catch ( ParseException ex ) {
			log.error( ex );

			throw ex;
		}
	}

	private static HVConfig loadConfig()
	{
		File configFile = new File( HVConfig.FILE_PATH );
		HVConfig config = null;

		if ( configFile.exists() ) {
			try {
				config = HVConfig.from( configFile );
			}
			catch ( Exception e ) {
				log.error( "Error while loading config file: ", e );
			}
		}
		else {
			config = new HVConfig();
		}

		return config;
	}

	private static void executeGUI( HVContext ctx, String subtitle, File inputFile, LoadedHierarchy.Options loadOptions )
	{
		HVConfig config = ctx.getConfig();

		// Attempt to set the application's Look and Feel
		boolean successLAF = false;
		String prefLAF = config.getPreferredLookAndFeel();

		if ( SystemUtils.IS_OS_UNIX && SwingUIUtils.isXFCE() && SwingUIUtils.isOpenJDK() ) {
			// Unix systems running XFCE desktop environment with OpenJDK experience a complete freeze
			// when running Java apps using the default Swing Look and Feel (Metal theme).
			// (specifically, when a second dialog window is being displayed).
			// Workaround is to use a different LAF.
			log.info( "Detected Unix system running XFCE desktop environment and OpenJDK." );

			if ( prefLAF == null || prefLAF.isEmpty() || prefLAF.equals( "Metal" ) ) {
				if ( config.isStopXfceLafChange() ) {
					log.trace( "Leaving LAF unchanged due to user override." );
				}
				else {
					log.info( "Using non-Metal LAF." );

					// Forcibly change LAF to something other than Metal
					try {
						prefLAF = Arrays.stream( UIManager.getInstalledLookAndFeels() )
							.filter( laf -> !laf.getName().toLowerCase( Locale.ENGLISH ).equals( "metal" ) )
							.findFirst()
							.get().getName();

						config.setPreferredLookAndFeel( prefLAF );
					}
					catch ( NoSuchElementException e ) {
						log.error( "No LAFs other than Metal are installed." );
					}
				}
			}
		}

		if ( prefLAF != null && !prefLAF.isEmpty() ) {
			try {
				// Try to use the user's preferred LAF, if we find it.
				final String fprefLAF = prefLAF;
				LookAndFeelInfo lafInfo = Arrays.stream( UIManager.getInstalledLookAndFeels() )
					.filter( laf -> laf.getName().equals( fprefLAF ) )
					.findFirst()
					.get();

				UIManager.setLookAndFeel( lafInfo.getClassName() );
				successLAF = true;
			}
			catch ( NoSuchElementException | ClassNotFoundException | UnsupportedLookAndFeelException e ) {
				log.printf(
					Level.ERROR,
					"Could not find a matching L&F for name '%s'. Falling back to system default.",
					prefLAF
				);
			}
			catch ( Exception ex ) {
				log.error( "Unexpected error occurred while setting preferred L&F: ", ex );
			}

			if ( !successLAF ) {
				log.printf(
					Level.INFO,
					"Could not find a matching L&F for name '%s'. Falling back to system default.",
					prefLAF
				);
			}
		}

		if ( !successLAF ) {
			// If the preferred L&F is not available, try to fall back to system default.

			try {
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( Exception ex ) {
				// If THAT failed, fall back to cross-platform, ie. default Swing look.
				log.info( "Could not set system default L&F. Falling back to cross-platform L&F." );

				try {
					UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
				}
				catch ( Exception exc ) {
					// Never happens.
					log.error( "Unexpected error occurred while setting cross-platform L&F: ", exc );
				}
			}

			// Set the preferred L&F to the current one, so that we don't have to look it up again,
			// and so that the correct L&F is selected in the config dialog.
			config.setPreferredLookAndFeel( UIManager.getLookAndFeel().getName() );
		}

		// Windows L&F uses Monospaced 9 as default font for JTextAreas, but other L&Fs don't.
		// Fix that by manually setting the default to the same font as JTextField.
		UIManager.getDefaults().put( "TextArea.font", UIManager.getFont( "TextField.font" ) );

		try {
			// Due to some internal workings of Swing, default sounds are not played for non-default L&Fs.
			// Details: stackoverflow.com/questions/12128231/12156617#12156617
			Object[] cueList = (Object[])UIManager.get( "AuditoryCues.cueList" );
			UIManager.put( "AuditoryCues.playList", cueList );
		}
		catch ( Exception e ) {
			log.error( "Unexpected error occurred while setting auditory cues list: ", e );
		}

		// Attempt to set the application name so that it displays correctly on all platforms.
		if ( SystemUtils.IS_OS_MAC ) {
			System.setProperty( "apple.awt.application.name", APP_NAME );
		}
		else if ( SystemUtils.IS_OS_LINUX ) {
			try {
				Toolkit xToolkit = Toolkit.getDefaultToolkit();
				Field awtAppClassNameField = xToolkit.getClass().getDeclaredField( "awtAppClassName" );
				awtAppClassNameField.setAccessible( true );
				awtAppClassNameField.set( xToolkit, APP_NAME );
			}
			catch ( Exception e ) {
			}
		}

		// Ensure all popups are triggered from the event dispatch thread.
		SwingUtilities.invokeLater( () -> initGUI( ctx, subtitle, inputFile, loadOptions ) );
	}

	private static void initGUI( HVContext ctx, String subtitle, File inputFile, LoadedHierarchy.Options loadOptions )
	{
		ctx.createGUI( subtitle );

		VisualizerFrame frame = ctx.getHierarchyFrame();
		frame.layoutFrames();
		frame.setVisible( true );
		frame.showFrames();

		if ( inputFile != null ) {
			SwingUtilities.invokeLater( () -> ctx.loadFile( frame, inputFile, loadOptions ) );
		}
	}

	/**
	 * Setup the file logger so that it outputs messages to the appropriately named file,
	 * depending on the subtitle of the current program instance.
	 */
	private static void configureLoggers( String subtitle )
	{
		// Remove all characters that are not allowed for Windows filenames.
		subtitle = subtitle.replaceAll( "[\\s" + Pattern.quote( "\\/:*?\"<>|" ) + "]", "" );

		LoggerContext context = (LoggerContext)LogManager.getContext();
		Configuration config = context.getConfiguration();

		PatternLayout layout = PatternLayout.createLayout(
			"%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%throwable%n",
			null, null, null, Charset.defaultCharset(), false, false, null, null
		);

		FileAppender appender = FileAppender.createAppender(
			"logs/log-" + subtitle + ".txt", "false", "false", "LogFile-" + subtitle, "true", "true", "true",
			"8192", layout, null, "false", "", config
		);

		org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger)LogManager.getRootLogger();
		rootLogger.addAppender( appender );
		appender.start();
	}

	public static void spawnNewInstance(
		String subtitle,
		File inputFile, boolean withTrueClass, boolean withInstanceNames ) throws IOException
	{
		List<String> argsList = new ArrayList<>();
		if ( inputFile != null ) {
			argsList.add( "-i" );
			argsList.add( inputFile.getPath() );
			if ( withTrueClass )
				argsList.add( "true-class" );
			if ( withInstanceNames )
				argsList.add( "instance-names" );
			argsList.add( "header" );
			// argsList.add( "fix-breadth-gaps" );
			// argsList.add( "use-subtree" );
		}
		if ( subtitle != null ) {
			argsList.add( "-s" );
			argsList.add( subtitle );
		}

		String[] args = argsList.toArray( new String[0] );
		if ( args.length == 0 ) args = null;

		Utils.start( HierarchyVisualizer.class, args );
	}
}
