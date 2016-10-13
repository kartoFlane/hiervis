package pl.pwr.hierarchyvis;

import java.io.File;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.reader.GeneratedCSVReader;
import pl.pwr.hierarchyvis.core.HVConfig;
import pl.pwr.hierarchyvis.core.HVContext;
import pl.pwr.hierarchyvis.core.HierarchyStatistics;
import pl.pwr.hierarchyvis.ui.VisualizerFrame;
import pl.pwr.hierarchyvis.util.CmdLineParser;
import pl.pwr.hierarchyvis.visualisation.HierarchyProcessor;


public final class HierarchyVisualizer {

	private HierarchyVisualizer() {
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	public static void main( String[] args ) {
		HVContext context = new HVContext();

		if ( args != null && args.length > 0 ) {
			System.out.println( "Args list is not empty -- running in CLI mode." );

			executeCLI( context, args );
		}
		else {
			System.out.println( "Args list is empty -- running in GUI mode." );

			executeGUI( context );
		}
	}

	private static void executeCLI( HVContext context, String[] args ) {
		try {
			CmdLineParser parser = new CmdLineParser();
			context.setConfig( parser.parse( args, context.getConfig() ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}

		HVConfig config = context.getConfig();
		Hierarchy inputData = null;

		if ( config.getInputDataFilePath().getFileName().endsWith( ".csv" ) ) {
			inputData = new GeneratedCSVReader().load(
					config.getInputDataFilePath().toString(),
					config.isClassAttribute(),
					false );
		}
		else {
			System.err.printf(
					"Unrecognised extension of input file: '%s', only *.csv files are supported.%n",
					config.getInputDataFilePath().getFileName() );
			System.exit( 1 );
		}

		String statsFilePath = config.getOutputFolder() + File.separator + config.getInputDataFilePath().getFileName();
		statsFilePath = statsFilePath.substring( 0, statsFilePath.lastIndexOf( "." ) ) + "_hieraryStatistics.csv";

		HierarchyStatistics stats = new HierarchyStatistics( inputData, statsFilePath );

		if ( !config.isSkipVisualisations() ) {
			HierarchyProcessor vis = new HierarchyProcessor();

			try {
				vis.process( context, stats );
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	private static void executeGUI( HVContext context ) {
		if ( context.getConfig().isUseNativeGUI() ) {
			try {
				// Swing is ugly. Use native L&F if possible.
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

				// Due to some internal working of Swing, default sounds are not played for non-default LAFs.
				// Details: stackoverflow.com/questions/12128231/12156617#12156617
				Object[] cueList = (Object[])UIManager.get( "AuditoryCues.cueList" );
				UIManager.put( "AuditoryCues.playList", cueList );
			}
			catch ( Exception e ) {
				System.err.printf( "Error occurred while setting system look and feel: %s", e );
			}
		}

		// Ensure all popups are triggered from the event dispatch thread.
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				initGUI( context );
			}
		} );
	}

	private static void initGUI( HVContext ctx ) {
		VisualizerFrame frame = new VisualizerFrame( ctx );
		frame.setVisible( true );
	}
}
