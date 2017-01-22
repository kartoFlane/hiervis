package pl.pwr.hiervis.util;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pl.pwr.hiervis.core.HVConfig;


public class CmdLineParser
{
	private CommandLineParser parser;
	private CommandLine cmd;
	private Options options;
	private HelpFormatter helpText;


	public CmdLineParser()
	{
		parser = new BasicParser();
		options = new Options();
		helpText = new HelpFormatter();
		createOptions();
	}

	@SuppressWarnings("static-access")
	private void createOptions()
	{
		String colorsString = "Possible values: { green, black, blue, lightBlue, yellow, " +
			"cyan, lightGray, gray, darkGray, magenta, orange, pink, red, white }, or " +
			"a hex string.";

		Option input = OptionBuilder
			.withArgName( "file path" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription( "path to file with input data" )
			.withLongOpt( "input" )
			.create( 'i' );

		Option output = OptionBuilder
			.withArgName( "directory path" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription( "path where *.PNG files showing each hierarchy level will be saved" )
			.withLongOpt( "output" )
			.create( 'o' );

		Option backgroundColor = OptionBuilder
			.withArgName( "color" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription( "background color of every output image." + colorsString + " Default: transparent." )
			.withLongOpt( "background-color" )
			.create( "bg" );

		Option currentGroup = OptionBuilder
			.withArgName( "color" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription(
				"color used to draw the current Level Group on the output images. " +
					colorsString + ". Default: red."
			)
			.withLongOpt( "current-level-group-color" )
			.create( "lg" );

		Option childGroup = OptionBuilder
			.withArgName( "color" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription(
				"color used to draw all Child Groups (successors) on the output images. " +
					colorsString + " Default: green."
			)
			.withLongOpt( "child-group-color" )
			.create( "cg" );

		Option parentGroup = OptionBuilder
			.withArgName( "color" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription(
				"color used to draw direct Parent Groups (ancestors) on the output images. " +
					colorsString + " Default: black. " +
					"In order to display these points, the -da flag should be set."
			)
			.withLongOpt( "parent-group-color" )
			.create( "pg" );

		Option parentAncestorsGroup = OptionBuilder
			.withArgName( "color" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription(
				"color used to draw Parents' direct ancestor groups on the output images. " +
					colorsString + " Default: lightBlue. " +
					"In order to display this points, the -da flag should be set."
			)
			.withLongOpt( "parent-group-color" )
			.create( "pa" );

		Option otherGroup = OptionBuilder
			.withArgName( "color" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription(
				"color used to draw all Child Groups (e.g. siblings) on the output images. " +
					colorsString + " Default: lightGray. " +
					"In order to display this points, the -da flag should be set."
			)
			.withLongOpt( "other-group-color" )
			.create( "og" );

		Option pointScale = OptionBuilder
			.withArgName( "real number" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription( "scaling factor (floating point number) of points drawn on images. Default: 1.0 (no scaling)" )
			.withLongOpt( "point-scale" )
			.create( "ps" );

		Option binsNumber = OptionBuilder
			.withArgName( "number of bins" )
			.hasArgs( 1 )
			.isRequired( false )
			.withDescription( "number of histogram bins. Default: 100." )
			.withLongOpt( "bins-number" )
			.create( "b" );

		Option help = OptionBuilder
			.withDescription( "prints this message" )
			.hasArg( false )
			.isRequired( false )
			.withLongOpt( "help" )
			.create( 'h' );

		options.addOption(
			"da",
			"display-all",
			false,
			"Display all points on the output images, so the other non-child groups " +
				"(e.g. siblings and all parent groups) are also displayed."
		);

		options.addOption(
			"c",
			"class-attribute",
			false,
			"Indicates that the provided input file also contains a ground-truth class attribute, " +
				"which will be omitted by this program. Assumed that class is in the second " +
				"column (attribute) in the input file."
		);

		options.addOption(
			"sv",
			"skip-visualisation",
			false,
			"The program will skip printing the visualisations as images. "
				+ "Only hierarchy statistics file will be produced."
		);

		options.addOption( input );
		options.addOption( output );
		options.addOption( backgroundColor );
		options.addOption( currentGroup );
		options.addOption( childGroup );
		options.addOption( parentGroup );
		options.addOption( parentAncestorsGroup );
		options.addOption( otherGroup );
		options.addOption( pointScale );
		options.addOption( binsNumber );
		options.addOption( help );
	}

	public HVConfig parse( String[] args )
		throws Exception
	{
		return parse( args, null );
	}

	public HVConfig parse( String[] args, HVConfig source )
		throws Exception
	{
		HVConfig config = null;

		if ( source == null ) {
			config = new HVConfig();
		}
		else {
			config = HVConfig.from( source );
		}

		try {
			cmd = parser.parse( options, args );
		}
		catch ( ParseException exp ) {
			System.err.println( exp.getMessage() );
			System.exit( 1 );
		}

		if ( cmd.hasOption( 'h' ) || cmd.hasOption( "help" ) || args.length == 0 ) {
			viewHelp();
			System.exit( 0 );
		}
		else {
			parseParameters( config );
		}

		return config;
	}

	private void parseParameters( HVConfig config )
		throws Exception
	{
		config.setInputDataFilePath( parseInputFile() );
		config.setOutputFolder( parseOutputFile() );

		config.setBackgroundColor( parseColor( cmd.getOptionValue( "bg", "transparent" ), true ) );
		config.setCurrentLevelColor( parseColor( cmd.getOptionValue( "lg", "red" ), false ) );
		config.setChildGroupColor( parseColor( cmd.getOptionValue( "cg", "green" ), false ) );
		config.setParentGroupColor( parseColor( cmd.getOptionValue( "pg", "blue" ), false ) );
		config.setAncestorGroupColor( parseColor( cmd.getOptionValue( "pa", "lightBlue" ), false ) );
		config.setOtherGroupColor( parseColor( cmd.getOptionValue( "og", "lightGray" ), false ) );

		config.setPointScallingFactor(
			parsePositiveDoubleParameter(
				cmd.getOptionValue( "ps", "1.0" ),
				"Points scalling factor should be a positive real number."
			)
		);

		config.setNumberOfHistogramBins(
			parsePositiveIntegerParameter(
				cmd.getOptionValue( "b", "100" ),
				"Number of bins should be a positive integer number."
			)
		);

		config.setDisplayAllPoints( cmd.hasOption( "da" ) );
		config.setTrueClassAttribute( cmd.hasOption( "c" ) );
		config.setSkipVisualisations( cmd.hasOption( "sv" ) );
	}

	private Color parseColor( String optionValue, boolean allowTransparent )
	{
		switch ( optionValue ) {
			case "green":
				return Color.green;
			case "black":
				return Color.black;
			case "blue":
				return Color.blue;
			case "lightBlue":
				return new Color( 0, 191, 255 );
			case "yellow":
				return Color.yellow;
			case "cyan":
				return Color.cyan;
			case "lightGray":
				return Color.lightGray;
			case "gray":
				return Color.gray;
			case "darkGray":
				return Color.darkGray;
			case "magenta":
				return Color.magenta;
			case "orange":
				return Color.orange;
			case "pink":
				return Color.pink;
			case "red":
				return Color.red;
			case "white":
				return Color.white;
			case "transparent": {
				if ( allowTransparent ) {
					return new Color( -1 );
				}
				else {
					throw new IllegalArgumentException( "Transparent color is not allowed for this option." );
				}
			}
			default: {
				if ( optionValue.startsWith( "#" ) ) {
					return Color.decode( optionValue );
				}
				else {
					throw new IllegalArgumentException(
						String.format(
							"Unknown color: '%s'. Allowed values: { " +
								"green, black, blue, lightBlue, yellow, cyan, lightGray," +
								"gray, darkGray, magenta, orange, pink, red, white }, or " +
								"a hex string.",
							optionValue
						)
					);
				}
			}
		}
	}

	private int parsePositiveIntegerParameter( String parsedOptionValue, String invalidArgMsg )
	{
		int parsedValue = -1;
		try {
			parsedValue = Integer.valueOf( parsedOptionValue );
			if ( parsedValue <= 0 ) {
				throw new NumberFormatException();
			}
		}
		catch ( NumberFormatException e ) {
			System.err.println(
				"'" + parsedOptionValue + "' " + invalidArgMsg
					+ " " + e.getMessage()
			);
			System.exit( -1 );
		}
		return parsedValue;
	}

	@SuppressWarnings("unused")
	private int parseIntegerParameter( String parsedOptionValue, String invalidArgMsg )
	{
		int parsedValue = -1;
		try {
			parsedValue = Integer.valueOf( parsedOptionValue );
		}
		catch ( NumberFormatException e ) {
			System.err.println(
				"'" + parsedOptionValue + "' " + invalidArgMsg
					+ " " + e.getMessage()
			);
			System.exit( -1 );
		}
		return parsedValue;
	}

	private double parsePositiveDoubleParameter( String parsedOptionValue, String invalidArgMsg )
	{
		double parsedValue = -1;
		try {
			parsedValue = Double.valueOf( parsedOptionValue );
			if ( parsedValue <= 0.0 ) {
				throw new NumberFormatException();
			}
		}
		catch ( NumberFormatException e ) {
			System.err.printf( "'%s' %s %s%n", parsedOptionValue, invalidArgMsg, e.getMessage() );
			System.exit( -1 );
		}
		return parsedValue;
	}

	@SuppressWarnings("unused")
	private double parseDoubleParameter( String parsedOptionValue, String invalidArgMsg )
	{
		double parsedValue = -1;
		try {
			parsedValue = Double.valueOf( parsedOptionValue );
		}
		catch ( NumberFormatException e ) {
			System.err.printf( "'%s' %s %s%n", parsedOptionValue, invalidArgMsg, e.getMessage() );
			System.exit( -1 );
		}
		return parsedValue;
	}

	private void viewHelp()
	{
		helpText.printHelp( "HierarchyVisualisation", options );
	}

	private Path parseInputFile()
		throws Exception
	{
		File inputFile = null;

		if ( cmd.hasOption( 'i' ) ) {
			inputFile = new File( cmd.getOptionValue( 'i' ) );

			if ( inputFile.isDirectory() ) {
				throw new IOException( inputFile.getPath() + " must be a path to a file!" );
			}
			if ( !inputFile.exists() ) {
				throw new FileNotFoundException( inputFile.getPath() );
			}
		}
		else {
			throw new Exception( "No input file specified! Use -i option." );
		}

		return inputFile.toPath();
	}

	private Path parseOutputFile()
		throws Exception
	{
		File outputFolder = null;

		if ( cmd.hasOption( 'o' ) ) {
			String outputFolderName = cmd.getOptionValue( 'o' );
			outputFolder = new File( outputFolderName );

			if ( outputFolder.isFile() ) {
				throw new IOException( outputFolder.getPath() + " must be a path to a directory!" );
			}
			if ( !outputFolder.exists() ) {
				System.out.println( outputFolderName + " doesn't exist, creating folder." );
				Files.createDirectories( outputFolder.toPath() );
			}
		}
		else {
			throw new Exception( "No output directory specified! Use -o option." );
		}

		return outputFolder.toPath();
	}
}
