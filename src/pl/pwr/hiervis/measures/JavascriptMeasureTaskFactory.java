package pl.pwr.hiervis.measures;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;


/**
 * Factory of {@link MeasureTask} objects created from Javascript files.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class JavascriptMeasureTaskFactory implements MeasureTaskFactory
{
	private static final Logger log = LogManager.getLogger( JavascriptMeasureTaskFactory.class );

	private ScriptEngine engine = null;


	/**
	 * 
	 * @param restrictedAccess
	 *            whether scripts eval'd by this factory should have restricted access to classes.
	 *            If true, scripts will only be able to access classes from the following packages:
	 *            <li>internal_measures</li>
	 *            <li>external_measures</li>
	 *            <li>distance_measures</li>
	 */
	public JavascriptMeasureTaskFactory( boolean restrictedAccess )
	{
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

		if ( restrictedAccess ) {
			// Apply a class filter for some semblance of security.
			engine = factory.getScriptEngine( JavascriptMeasureTaskFactory::isClassAccessibleFromScript );
		}
		else {
			engine = factory.getScriptEngine();
		}
	}

	/**
	 * Checks whether the {@link MeasureTask} script files are allowed to load the specified class.
	 * 
	 * @param classPath
	 *            fully qualified name of the class to check
	 * @return whether the specified class can be loaded by the script
	 */
	private static boolean isClassAccessibleFromScript( String classPath )
	{
		if ( classPath.startsWith( "internal_measures." )
			|| classPath.startsWith( "external_measures." )
			|| classPath.startsWith( "distance_measures." ) ) {
			return true;
		}

		return false;
	}

	@Override
	public MeasureTask getMeasureTask( Path path )
	{
		if ( Files.isDirectory( path ) ) {
			throw new IllegalArgumentException( "Path passed in argument must denote a single file!" );
		}
		return evalFile( engine, path );
	}

	@Override
	public Collection<MeasureTask> getMeasureTasks( Path path )
	{
		if ( Files.isDirectory( path ) ) {
			List<MeasureTask> result = new ArrayList<MeasureTask>();
			evalDirRecursive( engine, path, result );
			return result;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	private static void evalDirRecursive( ScriptEngine engine, Path path, Collection<MeasureTask> results )
	{
		if ( Files.isDirectory( path ) ) {
			try {
				Files.list( path ).forEach( p -> evalDirRecursive( engine, p, results ) );
			}
			catch ( IOException e ) {
				log.error( "Error while recursively eval'ing script directories:\n", e );
			}
		}
		else {
			results.add( evalFile( engine, path ) );
		}
	}

	private static MeasureTask evalFile( ScriptEngine engine, Path path )
	{
		Invocable inv = (Invocable)engine;

		JSObject scriptCallback = null;
		try {
			scriptCallback = (JSObject)engine.eval(
				new FileReader( path.toAbsolutePath().toFile() ),
				engine.createBindings()
			);
		}
		catch ( FileNotFoundException e ) {
			log.error( "Could not find MeasureTask script file: " + path.toString() );
		}
		catch ( ScriptException e ) {
			log.error( "Error while parsing MeasureTask script file: " + path.toString() + "\n", e );
		}

		try {
			if ( !scriptCallback.isFunction() ) {
				throw new IllegalArgumentException( "Return value of script is not a Function!" );
			}

			JSObject measureData = (JSObject)scriptCallback.call( null );
			String id = getMember( measureData, "id" );
			JSObject computeCallback = getMember( measureData, "callback" );

			boolean autoCompute = getOptionalMember( measureData, "autoCompute", false );
			JSObject applicabilityCallback = getOptionalMember( measureData, "isApplicable", null );

			if ( !computeCallback.isFunction() ) {
				throw new IllegalArgumentException( "Member 'callback' is not a Function!" );
			}
			if ( applicabilityCallback != null && !applicabilityCallback.isFunction() ) {
				throw new IllegalArgumentException( "Member 'isApplicable' is not a Function!" );
			}

			Function<Hierarchy, Boolean> applicabilityFunction = hierarchy -> {
				try {
					return (Boolean)inv.invokeMethod( measureData, "isApplicable", hierarchy );
				}
				catch ( NoSuchMethodException e ) {
					return true;
				}
				catch ( Throwable e ) {
					log.error(
						String.format( "Unexpected rrror while invoking applicability callback for measure '%s': ", id ), e
					);
				}
				return false;
			};

			Function<Hierarchy, Object> computeFunction = hierarchy -> {
				try {
					return inv.invokeMethod( measureData, "callback", hierarchy );
				}
				catch ( NoSuchMethodException | ScriptException e ) {
					log.error(
						String.format( "Error while invoking compute callback for measure '%s': ", id ), e
					);
				}
				return null;
			};

			return new MeasureTask( id, autoCompute, applicabilityFunction, computeFunction );
		}
		catch ( IllegalArgumentException e ) {
			log.error(
				String.format(
					"Error while constructing MeasureTask from script file '%s': %s",
					path.toString(), e.getMessage()
				)
			);
		}
		catch ( NoSuchFieldException e ) {
			log.error(
				String.format(
					"Error while constructing MeasureTask from script file '%s':"
						+ " Could not find member '%s' in returned object.",
					path.toString(), e.getMessage()
				)
			);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getOptionalMember( JSObject jsObject, String member, T defaultValue )
	{
		return jsObject.hasMember( member )
			? (T)jsObject.getMember( member )
			: defaultValue;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getMember( JSObject jsObject, String member ) throws NoSuchFieldException
	{
		if ( jsObject.hasMember( member ) )
			return (T)jsObject.getMember( member );
		else
			throw new NoSuchFieldException( member );
	}
}
