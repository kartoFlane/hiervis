function() {
	// Load required classes
	var LeavesPerLevel = Java.type( 'internal_measures.statistics.histogram.LeavesPerLevel' );

	// Initialize the measure object
	var measure = new LeavesPerLevel();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Leaves Per Level';
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
