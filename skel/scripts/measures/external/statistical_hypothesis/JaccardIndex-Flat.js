function() {
	// Load required classes
	var JaccardIndex = Java.type( 'external_measures.statistical_hypothesis.JaccardIndex' );
	var FlatHypotheses = Java.type( 'external_measures.statistical_hypothesis.FlatHypotheses' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new JaccardIndex( new FlatHypotheses() );
	measureData.id = 'Jaccard Index (Flat)';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
