package pl.pwr.hiervis.measures;

import java.nio.file.Path;
import java.util.Collection;


/**
 * Interface for a factory of {@link MeasureTask} objects.
 * 
 * @author Tomasz Bachmiński
 *
 */
public interface MeasureTaskFactory
{
	/**
	 * Creates a new {@link MeasureTask} based on the definition in the specified file.
	 * 
	 * @param path
	 *            path to a single file, containing definition for a single {@link MeasureTask} object.
	 * @return a {@link MeasureTask} object created from the specified file.
	 */
	MeasureTask getMeasureTask( Path path );

	/**
	 * Creates several new {@link MeasureTask}s based on the definitions in the specified file, or folder.
	 * 
	 * @param path
	 *            path to a single file containing multiple {@link MeasureTask} definitions, or to a folder
	 *            containing files with such definitions.
	 * @return a collection of {@link MeasureTask}s created from the specified file or folder.
	 */
	Collection<MeasureTask> getMeasureTasks( Path path );
}
