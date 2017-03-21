function createMeasureData() {
	// Load required classes
	var VarianceDeviation = Java.type( 'internal_measures.VarianceDeviation' );

	// Initialize the measure object
	var measure = new VarianceDeviation( 1.0 );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Variance Deviation';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
