function() {
	// Load required classes
	var RandIndex = Java.type( 'external_measures.statistical_hypothesis.RandIndex' );
	var FlatHypotheses = Java.type( 'external_measures.statistical_hypothesis.FlatHypotheses' );

	// Initialize the measure object
	var measure = new RandIndex( new FlatHypotheses() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Rand Index (Flat)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
