package pl.pwr.hiervis.core;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.pwr.hiervis.util.CmdLineParser;


/**
 * Class storing the configuration of the visualizer at runtime
 * for easy access.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class HVConfig
{
	/**
	 * Default path to the config file
	 */
	public static final String FILE_PATH = "./config.json";

	private static final Logger log = LogManager.getLogger( HVConfig.class );

	@SerializableField
	private Path inputDataFilePath;
	@SerializableField
	private Path outputFolder;
	@SerializableField
	private Color currentGroupColor;
	@SerializableField
	private Color childGroupColor;
	@SerializableField
	private Color parentGroupColor;
	@SerializableField
	private Color ancestorGroupColor;
	@SerializableField
	private Color otherGroupColor;
	@SerializableField
	private Color backgroundColor;
	@SerializableField
	private int treeResolutionWidth;
	@SerializableField
	private int treeResolutionHeight;
	@SerializableField
	private int pointResolutionWidth;
	@SerializableField
	private int pointResolutionHeight;
	@SerializableField
	private double pointScalingFactor;
	@SerializableField
	private int numberOfHistogramBins;
	@SerializableField
	private boolean displayAllPoints;
	@SerializableField
	private boolean trueClassAttribute;
	@SerializableField
	private boolean instanceNameAttribute;
	@SerializableField
	private boolean dataNamesRow;
	@SerializableField
	private boolean fillBreadthGaps;
	@SerializableField
	private boolean skipVisualisations;
	@SerializableField
	private boolean useSubtree;
	@SerializableField
	private boolean useTrueClass;
	@SerializableField
	private String preferredLookAndFeel;


	/**
	 * Create a new config with default values.
	 */
	public HVConfig()
	{
		// Setup default values.
		treeResolutionWidth = 1000;
		treeResolutionHeight = 1000;
		pointResolutionWidth = 600;
		pointResolutionHeight = 600;
		numberOfHistogramBins = 5;
		pointScalingFactor = 1;

		currentGroupColor = Color.red;
		childGroupColor = Color.green;
		parentGroupColor = Color.black;
		ancestorGroupColor = Color.blue.brighter();
		otherGroupColor = Color.lightGray;
		backgroundColor = new Color( -1 );

		displayAllPoints = true;
		trueClassAttribute = false;
		instanceNameAttribute = false;
		skipVisualisations = false;
		useSubtree = true;
		preferredLookAndFeel = "";
	}

	/**
	 * Create a shallow copy of the specified source config.
	 * 
	 * @param source
	 *            config to copy values from
	 * @return the new, copied config (shallow copy)
	 */
	public static HVConfig from( HVConfig source )
	{
		if ( source == null ) {
			throw new IllegalArgumentException( "Source config must not be null!" );
		}

		HVConfig clone = new HVConfig();

		try {
			for ( Field field : HVConfig.class.getDeclaredFields() ) {
				if ( isValidField( field ) ) {
					// We're setting corresponding fields, so there's no need to use HVConfig.setField().
					field.set( clone, field.get( source ) );
				}
			}
		}
		catch ( IllegalArgumentException | IllegalAccessException e ) {
			log.error( "Error while processing config fields: ", e );
		}

		return clone;
	}

	/**
	 * Create a new config from the specified file.
	 * 
	 * @param file
	 *            the file containing config values. Assumed to be in *.json format.
	 * @return the loaded confing
	 * @throws IOException
	 *             if an IO error occurred
	 * @throws JsonProcessingException
	 *             if an error occurred while processing the json text
	 */
	public static HVConfig from( File file )
		throws IOException
	{
		HVConfig config = new HVConfig();

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure( JsonParser.Feature.ALLOW_COMMENTS, true );
		mapper.configure( JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true );

		JsonNode rootNode = mapper.readTree( file );

		try {
			for ( Field field : HVConfig.class.getDeclaredFields() ) {
				JsonNode node = rootNode.get( field.getName() );
				if ( node != null && node instanceof NullNode == false && isValidField( field ) ) {
					config.setField( field, node );
				}
			}
		}
		catch ( IllegalArgumentException | IllegalAccessException e ) {
			log.error( "Error while processing config fields: ", e );
		}

		return config;
	}

	/**
	 * Create a new config from the specified arguments array, with
	 * an optional source config.
	 * 
	 * @param args
	 *            the array of arguments
	 * @param source
	 *            the source config (optional)
	 * @return the loaded config
	 * @throws Exception
	 *             if an error occurred while parsing the arguments
	 */
	public static HVConfig from( String[] args, HVConfig source )
		throws Exception
	{
		CmdLineParser parser = new CmdLineParser();
		return parser.parse( args, source );
	}

	/**
	 * Create a shallow copy of the receiver.
	 * 
	 * @see HVConfig#from(HVConfig)
	 */
	public HVConfig copy()
	{
		return HVConfig.from( this );
	}

	/**
	 * Serializes this config object, saving it to the specified file in JSON format.
	 * 
	 * @param file
	 *            the file to save the config to
	 */
	public void to( File file )
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure( JsonParser.Feature.ALLOW_COMMENTS, true );
		mapper.configure( JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true );

		ObjectNode root = mapper.createObjectNode();

		try {
			JsonNodeFactory f = JsonNodeFactory.withExactBigDecimals( false );

			for ( Field field : HVConfig.class.getDeclaredFields() ) {
				if ( isValidField( field ) ) {
					root.set( field.getName(), serializeField( f, field ) );
				}
			}
		}
		catch ( IllegalArgumentException | IllegalAccessException e ) {
			log.error( "Error while processing config fields: ", e );
		}

		try {
			ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
			writer.writeValue( file, root );
		}
		catch ( Exception e ) {
			log.error( "Error while writing json data to file: ", e );
		}
	}

	/**
	 * Checks whether the field is 'valid' for the purpose of config serialization.
	 * 
	 * @param f
	 *            the field to check
	 * @return true if the field is marked with the SerializableField annnotation.
	 */
	private static boolean isValidField( Field f )
	{
		return f.isAnnotationPresent( SerializableField.class );
	}

	/**
	 * Set the specified field to the value represented by the specified
	 * JSON node, converted to appropriate type (depending on field type)
	 * 
	 * @param f
	 *            the field to set
	 * @param node
	 *            the node containing the value for the field
	 * @throws IllegalArgumentException
	 *             if the value represented by the node is not the
	 *             appropriate type for the field, or no case has
	 *             been implemented to handle that field's type.
	 * @throws IllegalAccessException
	 *             if the specified field cannot be accessed
	 */
	private void setField( Field f, JsonNode node )
		throws IllegalArgumentException, IllegalAccessException
	{
		if ( f.getType().equals( boolean.class ) ) {
			f.set( this, node.asBoolean() );
		}
		else if ( f.getType().equals( int.class ) ) {
			f.set( this, node.asInt() );
		}
		else if ( f.getType().equals( long.class ) ) {
			f.set( this, node.asLong() );
		}
		else if ( f.getType().equals( double.class ) ) {
			f.set( this, node.asDouble() );
		}
		else if ( f.getType().equals( float.class ) ) {
			float value = (float)node.asDouble();
			f.set( this, value );
		}
		else if ( f.getType().equals( String.class ) ) {
			f.set( this, node.asText() );
		}
		else if ( f.getType().equals( Path.class ) ) {
			File value = new File( node.asText() );
			f.set( this, value.toPath() );
		}
		else if ( f.getType().equals( Color.class ) ) {
			String input = node.asText();
			try {
				Field cf = Color.class.getDeclaredField( input );
				int m = cf.getModifiers();
				// Get only publically available static fields, so that we only permit
				// accessing color constants by name, like 'red'
				if ( Modifier.isPublic( m ) && Modifier.isStatic( m ) ) {
					f.set( this, cf.get( null ) );
				}
				else {
					throw new IllegalArgumentException(
						String.format(
							"'%s' is not a valid color constant!", input
						)
					);
				}
			}
			catch ( Exception e ) {
				try {
					Color value = Color.decode( input );
					f.set( this, value );
				}
				catch ( NumberFormatException ex ) {
					log.error( "Error while processing value for a color field: ", e );
				}
			}
		}
		else {
			throw new IllegalArgumentException(
				String.format(
					"No case defined for field type %s",
					f.getType().getSimpleName()
				)
			);
		}
	}

	/**
	 * Serializes the specified field into a JsonNode of the appropriate type
	 * created by the specified factory object.
	 * 
	 * @param factory
	 *            the factory object which creates JSON nodes
	 * @param f
	 *            the field to serialize
	 * @return a JsonNode instance representing the specified field
	 * @throws IllegalArgumentException
	 *             if the value represented by the node is not the
	 *             appropriate type for the field, or no case has
	 *             been implemented to handle that field's type.
	 * @throws IllegalAccessException
	 *             if the specified field cannot be accessed
	 */
	private JsonNode serializeField( JsonNodeFactory factory, Field f )
		throws IllegalArgumentException, IllegalAccessException
	{
		if ( f.getType().equals( boolean.class ) ) {
			return factory.booleanNode( f.getBoolean( this ) );
		}
		else if ( f.getType().equals( int.class ) ) {
			return factory.numberNode( f.getInt( this ) );
		}
		else if ( f.getType().equals( long.class ) ) {
			return factory.numberNode( f.getLong( this ) );
		}
		else if ( f.getType().equals( double.class ) ) {
			return factory.numberNode( f.getDouble( this ) );
		}
		else if ( f.getType().equals( float.class ) ) {
			return factory.numberNode( f.getFloat( this ) );
		}
		else if ( f.getType().equals( String.class ) ) {
			return factory.textNode( (String)f.get( this ) );
		}
		else if ( f.getType().equals( Path.class ) ) {
			Path value = (Path)f.get( this );
			return factory.textNode( value == null ? null : value.toString() );
		}
		else if ( f.getType().equals( Color.class ) ) {
			Color value = (Color)f.get( this );
			return factory.textNode(
				String.format(
					"#%02X%02X%02X", // Format as uppercase hex string.
					value.getRed(),
					value.getGreen(),
					value.getBlue()
				)
			);
		}
		else {
			throw new IllegalArgumentException(
				String.format(
					"No case defined for field type %s",
					f.getType().getSimpleName()
				)
			);
		}
	}

	/*
	 * -----------------------------------
	 * Config values' getters and setters.
	 */

	public Color getCurrentGroupColor()
	{
		return currentGroupColor;
	}

	public void setCurrentLevelColor( Color currentGroupColor )
	{
		this.currentGroupColor = currentGroupColor;
	}

	public Color getChildGroupColor()
	{
		return childGroupColor;
	}

	public void setChildGroupColor( Color childGroupsColor )
	{
		this.childGroupColor = childGroupsColor;
	}

	public Color getParentGroupColor()
	{
		return parentGroupColor;
	}

	public void setParentGroupColor( Color parentGroupsColor )
	{
		this.parentGroupColor = parentGroupsColor;
	}

	public Color getOtherGroupColor()
	{
		return otherGroupColor;
	}

	public void setOtherGroupColor( Color otherGroupsColor )
	{
		this.otherGroupColor = otherGroupsColor;
	}

	public Color getBackgroundColor()
	{
		return backgroundColor;
	}

	public void setBackgroundColor( Color backgroudColor )
	{
		this.backgroundColor = backgroudColor;
	}

	public Color getAncestorGroupColor()
	{
		return ancestorGroupColor;
	}

	public void setAncestorGroupColor( Color ancestorColor )
	{
		this.ancestorGroupColor = ancestorColor;
	}

	public int getTreeWidth()
	{
		return treeResolutionWidth;
	}

	public void setTreeWidth( int imageWidth )
	{
		this.treeResolutionWidth = imageWidth;
	}

	public int getTreeHeight()
	{
		return treeResolutionHeight;
	}

	public void setTreeHeight( int imageHeight )
	{
		this.treeResolutionHeight = imageHeight;
	}

	public int getInstanceWidth()
	{
		return pointResolutionWidth;
	}

	public void setPointWidth( int imageWidth )
	{
		this.pointResolutionWidth = imageWidth;
	}

	public int getInstanceHeight()
	{
		return pointResolutionHeight;
	}

	public void setPointHeight( int imageHeight )
	{
		this.pointResolutionHeight = imageHeight;
	}

	public double getPointScallingFactor()
	{
		return pointScalingFactor;
	}

	public void setPointScallingFactor( double pointScallingFactor )
	{
		this.pointScalingFactor = pointScallingFactor;
	}

	public boolean isDisplayAllPoints()
	{
		return displayAllPoints;
	}

	public void setDisplayAllPoints( boolean displayAllPoints )
	{
		this.displayAllPoints = displayAllPoints;
	}

	public Path getInputDataFilePath()
	{
		return inputDataFilePath;
	}

	public void setInputDataFilePath( Path path )
	{
		this.inputDataFilePath = path;
	}

	public Path getOutputFolder()
	{
		return outputFolder;
	}

	public void setOutputFolder( Path path )
	{
		this.outputFolder = path;
	}

	public boolean hasTrueClassAttribute()
	{
		return trueClassAttribute;
	}

	public void setTrueClassAttribute( boolean classAttribute )
	{
		this.trueClassAttribute = classAttribute;
	}

	public boolean hasInstanceNameAttribute()
	{
		return instanceNameAttribute;
	}

	public void setInstanceNameAttribute( boolean instanceNameAttribute )
	{
		this.instanceNameAttribute = instanceNameAttribute;
	}

	public boolean hasDataNamesRow()
	{
		return dataNamesRow;
	}

	public void setDataNamesRow( boolean dataNames )
	{
		this.dataNamesRow = dataNames;
	}

	public boolean isFillBreadthGaps()
	{
		return fillBreadthGaps;
	}

	public void setFillBreadthGaps( boolean fillBreadthGaps )
	{
		this.fillBreadthGaps = fillBreadthGaps;
	}

	public void setNumberOfHistogramBins( int numberOfHistogramBins )
	{
		this.numberOfHistogramBins = numberOfHistogramBins;
	}

	public int getNumberOfHistogramBins()
	{
		return numberOfHistogramBins;
	}

	public void setSkipVisualisations( boolean skipVisualisations )
	{
		this.skipVisualisations = skipVisualisations;
	}

	public boolean hasSkipVisualisations()
	{
		return this.skipVisualisations;
	}

	public void setUseSubtree( boolean useSubtree )
	{
		this.useSubtree = useSubtree;
	}

	public boolean isUseSubtree()
	{
		return this.useSubtree;
	}

	public void setUseTrueClass( boolean useTrueClass )
	{
		this.useTrueClass = useTrueClass;
	}

	public boolean isUseTrueClass()
	{
		return this.useTrueClass;
	}

	public void setPreferredLookAndFeel( String lookAndFeel )
	{
		preferredLookAndFeel = lookAndFeel;
	}

	public String getPreferredLookAndFeel()
	{
		return preferredLookAndFeel;
	}

	/*
	 * ----------------------
	 * Miscellaneous methods.
	 */

	@Override
	public boolean equals( Object o )
	{
		if ( o == null )
			return false;
		if ( o instanceof HVConfig == false )
			return false;
		return equals( (HVConfig)o );
	}

	public boolean equals( HVConfig o )
	{
		try {
			for ( Field field : HVConfig.class.getDeclaredFields() ) {
				if ( isValidField( field ) ) {
					Object lv = field.get( this );
					Object rv = field.get( o );

					if ( !Objects.equals( lv, rv ) )
						return false;
				}
			}
		}
		catch ( IllegalArgumentException | IllegalAccessException e ) {
			log.error( "Error while processing config fields: ", e );
		}

		return true;
	}


	/**
	 * Marker annotation used to distinguish fields that are meant to be
	 * serialized into the config file.
	 * 
	 * @author Tomasz Bachmiński
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	private @interface SerializableField
	{
	}
}
