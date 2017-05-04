function() {
	// Load required classes
	var FowlkesMallowsIndex = Java.type( 'external_measures.statistical_hypothesis.FowlkesMallowsIndex' );
	var FlatHypotheses = Java.type( 'external_measures.statistical_hypothesis.FlatHypotheses' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new FowlkesMallowsIndex( new FlatHypotheses() );
	measureData.id = 'Fowlkes-Mallows Index (Flat)';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
