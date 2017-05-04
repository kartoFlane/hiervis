function() {
	// Load required classes
	var RandIndex = Java.type( 'external_measures.statistical_hypothesis.RandIndex' );
	var FlatHypotheses = Java.type( 'external_measures.statistical_hypothesis.FlatHypotheses' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new RandIndex( new FlatHypotheses() );
	measureData.id = 'Rand Index (Flat)';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
