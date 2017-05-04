function() {
	// Load required classes
	var VarianceDeviation = Java.type( 'internal_measures.VarianceDeviation' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new VarianceDeviation( 1.0 );
	measureData.id = 'Variance Deviation (1.0)';
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
