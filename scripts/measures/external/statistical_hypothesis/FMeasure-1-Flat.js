function() {
	// Load required classes
	var Fmeasure = Java.type( 'external_measures.statistical_hypothesis.Fmeasure' );
	var FlatHypotheses = Java.type( 'external_measures.statistical_hypothesis.FlatHypotheses' );

	// Initialize the measure object
	var beta = 1.0;
	var measure = new Fmeasure( beta, new FlatHypotheses() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'F-Measure (1.0, Flat)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
