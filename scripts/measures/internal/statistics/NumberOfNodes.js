function() {
	// Load required classes
	var NumberOfNodes = Java.type( 'internal_measures.statistics.NumberOfNodes' );

	// Initialize the measure object
	var measure = new NumberOfNodes();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Number of Nodes';
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
