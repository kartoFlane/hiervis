function createMeasureData() {
	// Load required classes
	var FowlkesMallowsIndex = Java.type( 'external_measures.statistical_hypothesis.FowlkesMallowsIndex' );
	var FlatHypotheses = Java.type( 'external_measures.statistical_hypothesis.FlatHypotheses' );

	// Initialize the measure object
	var measure = new FowlkesMallowsIndex( new FlatHypotheses() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Fowlkes-Mallows Index (Flat)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
