package pl.pwr.hiervis;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

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

		if ( args != null && args.length > 0 ) {
			log.info( "Args list is not empty -- running in CLI mode." );

			executeCLI( context, args );
		}
		else {
			log.info( "Args list is empty -- running in GUI mode." );

			executeGUI( context );
		}
	}

	private static void executeCLI( HVContext context, String[] args )
	{
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
					false
				);
			}
			catch ( IOException e ) {
				System.err.println( "Error while reading input file:" );
				e.printStackTrace();
			}
		}
		else {
			log.error(
				"Unrecognised extension of input file: '%s', only *.csv files are supported.%n",
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

		if ( prefLAF != null && !prefLAF.isEmpty() ) {
			try {
				// Try to use the user's preferred LAF, if we find it.
				for ( LookAndFeelInfo info : UIManager.getInstalledLookAndFeels() ) {
					if ( info.getName().equals( config.getPreferredLookAndFeel() ) ) {
						UIManager.setLookAndFeel( info.getClassName() );
						successLAF = true;
						break;
					}
				}
			}
			catch ( ClassNotFoundException | UnsupportedLookAndFeelException e ) {
				log.error(
					"Could not find a matching LAF for name '%s'. Falling back to system default.%n",
					config.getPreferredLookAndFeel()
				);
			}
			catch ( Exception ex ) {
				log.error( "Error occurred while setting preferred LAF: %s", ex );
			}

			if ( !successLAF ) {
				log.info(
					"Could not find a matching LAF for name '%s'. Falling back to system default.%n",
					config.getPreferredLookAndFeel()
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
				log.info( "Could not set system default LAF. Falling back to cross-platform LAF.%n" );

				try {
					UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
				}
				catch ( Exception exc ) {
					// Never happens.
					log.error( "Error occurred while setting cross-platform LAF: %s", exc );
				}
			}

			// Set the preferred LAF to the current one, so that we don't have to look it up again,
			// and so that the correct LAF is selected in the config dialog.
			config.setPreferredLookAndFeel( UIManager.getLookAndFeel().getName() );
		}

		try {
			// Due to some internal workings of Swing, default sounds are not played for non-default LAFs.
			// Details: stackoverflow.com/questions/12128231/12156617#12156617
			Object[] cueList = (Object[])UIManager.get( "AuditoryCues.cueList" );
			UIManager.put( "AuditoryCues.playList", cueList );
		}
		catch ( Exception e ) {
			log.error( "Error occurred while setting auditory cues list: %s", e );
		}

		// Attempt to set the application name so that it displays correctly on all platforms.
		// Mac
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", APP_NAME );
		System.setProperty( "apple.awt.application.name", APP_NAME );

		// Linux
		try {
			Toolkit xToolkit = Toolkit.getDefaultToolkit();
			Field awtAppClassNameField = xToolkit.getClass().getDeclaredField( "awtAppClassName" );
			awtAppClassNameField.setAccessible( true );
			awtAppClassNameField.set( xToolkit, APP_NAME );
		}
		catch ( Exception e ) {
			log.trace( "Failed to set app name via toolkit reflection." );
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
		VisualizerFrame frame = new VisualizerFrame( ctx );
		frame.setVisible( true );
	}
}
