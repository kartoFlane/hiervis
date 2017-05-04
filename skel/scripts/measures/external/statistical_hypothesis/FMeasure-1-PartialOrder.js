function() {
	// Load required classes
	var Fmeasure = Java.type( 'external_measures.statistical_hypothesis.Fmeasure' );
	var PartialOrderHypothesis = Java.type( 'external_measures.statistical_hypothesis.PartialOrderHypotheses' );

	var beta = 1.0;

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new Fmeasure( beta, new PartialOrderHypothesis() );
	measureData.id = 'F-Measure (1.0, Partial Order)';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
