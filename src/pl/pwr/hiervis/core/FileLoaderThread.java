package pl.pwr.hiervis.core;

import java.io.File;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.common.Utils;
import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.reader.GeneratedCSVReader;
import pl.pwr.hiervis.util.Event;


public class FileLoaderThread extends Thread
{
	private static final Logger log = LogManager.getLogger( FileLoaderThread.class );

	public final Event<Pair<File, LoadedHierarchy>> fileLoaded = new Event<>();
	public final Event<Exception> errorOcurred = new Event<>();

	private final File file;
	private final LoadedHierarchy.Options options;

	private GeneratedCSVReader reader;


	/**
	 * 
	 * @param config
	 *            HVConfig object describing parameters to use while loading the file
	 * @param file
	 *            the file to load (CSV format)
	 */
	public FileLoaderThread( File file, LoadedHierarchy.Options options )
	{
		setName( "FileLoaderThread" );
		setDaemon( true );

		this.file = file;
		this.options = options;
	}

	@Override
	public void run()
	{
		log.trace( "File loader thread started." );

		try {
			log.trace( "Parsing..." );

			reader = new GeneratedCSVReader();

			Hierarchy hierarchy = reader.load(
				file.getAbsolutePath(),
				options.hasTnstanceNameAttribute,
				options.hasTrueClassAttribute,
				options.hasColumnHeader,
				options.isFillBreadthGaps,
				options.isUseSubtree
			);

			log.trace( "Verifying..." );
			verify( hierarchy );

			LoadedHierarchy lh = new LoadedHierarchy( hierarchy, options );

			fileLoaded.broadcast( Pair.of( file, lh ) );
		}
		catch ( Utils.RuntimeInterruptedException ex ) {
			log.trace( "File loading aborted by user." );
		}
		catch ( Exception ex ) {
			log.error( "Error ocurred while loading " + file.getAbsolutePath() + "\n", ex );
			errorOcurred.broadcast( ex );
		}

		fileLoaded.clearListeners();
		errorOcurred.clearListeners();

		log.trace( "File loader thread finished." );
	}

	public int getProgress()
	{
		return reader == null ? 0 : reader.getProgress();
	}

	public String getStatusMessage()
	{
		return reader == null ? "" : reader.getStatusMessage();
	}

	/**
	 * Perform various tests on the hierarchy, meant to detect whether the input file
	 * has been loaded with correct options selected.
	 * 
	 * @param h
	 *            the hierarchy to verify
	 */
	private void verify( Hierarchy h )
	{
		int dataDims = h.getRoot().getSubtreeInstances().getFirst().getData().length;

		if ( dataDims == 1 ) {
			throw new RuntimeException( "Instance data only has 1 feature. Minimum of 2 are required." );
		}

		String[] dataNames = h.getDataNames();
		if ( dataNames != null ) {
			for ( int i = 0; i < dataNames.length; ++i ) {
				try {
					Double.parseDouble( dataNames[i] );
					// It parsed as a double, so most likely incorrect settings were selected.
					throw new RuntimeException(
						"One of instance feature column names parsed as a number. " +
							"Incorrect settings were likely selected."
					);
				}
				catch ( NumberFormatException e ) {
					// If it failed to parse as double, then it's likely some identifier. All's good.
				}
			}
		}
	}
}
