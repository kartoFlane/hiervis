function createMeasureData() {
	// Load required classes
	var Height = Java.type( 'internal_measures.statistics.Height' );

	// Initialize the measure object
	var measure = new Height();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Height';
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
