function createMeasureData() {
	// Load required classes
	var VarianceDeviation2 = Java.type( 'internal_measures.VarianceDeviation2' );

	// Initialize the measure object
	var measure = new VarianceDeviation2();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Variance Deviation 2';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
