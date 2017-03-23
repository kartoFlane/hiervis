function() {
	// Load required classes
	var AvgPathLength = Java.type( 'internal_measures.statistics.AvgPathLength' );

	// Initialize the measure object
	var measure = new AvgPathLength();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Average Path Length';
	measureData.autoCompute = true;
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
