package pl.pwr.hiervis;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.reader.GeneratedCSVReader;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.core.HierarchyStatistics;
import pl.pwr.hiervis.ui.VisualizerFrame;
import pl.pwr.hiervis.util.CmdLineParser;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;


public final class HierarchyVisualizer
{
	private static final Logger log = LogManager.getLogger( HierarchyVisualizer.class );

	public static final String APP_NAME = "Hierarchy Visualizer";


	private HierarchyVisualizer()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	public static void main( String[] args )
	{
		HVContext context = new HVContext();

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

		context.setConfig( loadConfig() );

		if ( args != null && args.length > 0 ) {
			log.info( "Args list is not empty -- running in CLI mode." );

			executeCLI( context, args );
		}
		else {
			log.info( "Args list is empty -- running in GUI mode." );

			executeGUI( context );
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

	private static void executeCLI( HVContext context, String[] args )
	{
		// TODO: Rework this whole method, or just scrap it.

		try {
			CmdLineParser parser = new CmdLineParser();
			context.setConfig( parser.parse( args, context.getConfig() ) );
		}
		catch ( Exception e ) {
			log.error( e );
		}

		HVConfig config = context.getConfig();
		Hierarchy inputData = null;

		if ( config.getInputDataFilePath().getFileName().endsWith( ".csv" ) ) {
			try {
				inputData = new GeneratedCSVReader().load(
					config.getInputDataFilePath().toString(),
					config.hasInstanceNameAttribute(),
					config.hasTrueClassAttribute(),
					config.hasDataNamesRow(),
					false, true
				);
			}
			catch ( IOException e ) {
				log.error( "Error while reading input file: ", e );
			}
		}
		else {
			log.printf(
				Level.ERROR,
				"Unrecognised extension of input file: '%s', only *.csv files are supported.",
				config.getInputDataFilePath().getFileName()
			);
			System.exit( 1 );
		}

		// TODO: Correct handling of stats
		String statsFilePath = config.getOutputFolder() + File.separator + config.getInputDataFilePath().getFileName();
		statsFilePath = statsFilePath.substring( 0, statsFilePath.lastIndexOf( "." ) ) + "_hieraryStatistics.csv";

		HierarchyStatistics stats = new HierarchyStatistics( inputData, statsFilePath );

		// TODO: Visualizations saved to images
		if ( !config.hasSkipVisualisations() ) {
			HierarchyProcessor vis = new HierarchyProcessor();

			try {
				// vis.process( context, stats );
			}
			catch ( Exception e ) {
				log.error( e );
			}
		}
	}

	private static void executeGUI( HVContext ctx )
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
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run()
				{
					initGUI( ctx );
				}
			}
		);
	}

	private static void initGUI( HVContext ctx )
	{
		ctx.createGUI();

		VisualizerFrame frame = ctx.getHierarchyFrame();
		frame.layoutFrames();
		frame.setVisible( true );
		frame.showFrames();
	}

	public static void spawnNewInstance() throws IOException
	{
		Process process = Utils.start( HierarchyVisualizer.class );
	}
}
