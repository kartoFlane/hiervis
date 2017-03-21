function createMeasureData() {
	// Load required classes
	var ChildPerNodePerLevel = Java.type( 'internal_measures.statistics.histogram.ChildPerNodePerLevel' );

	// Initialize the measure object
	var measure = new ChildPerNodePerLevel();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Children Per Node Per Level';
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
