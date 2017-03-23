function() {
	// Load required classes
	var NumberOfLeaves = Java.type( 'internal_measures.statistics.NumberOfLeaves' );

	// Initialize the measure object
	var measure = new NumberOfLeaves();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Number of Leaves';
	measureData.autoCompute = true;
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
