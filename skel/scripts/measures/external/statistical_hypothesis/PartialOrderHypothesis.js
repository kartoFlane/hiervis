function() {
	// Load required classes
	var PartialOrderHypothesis = Java.type( 'external_measures.statistical_hypothesis.PartialOrderHypotheses' );

	// Initialize the measure object
	var measure = new PartialOrderHypothesis();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Partial Order Hypothesis';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		measure.calculate( hierarchy );
		return "TP: " + measure.getTP() + "\n" +
			"FP: " + measure.getFP() + "\n" +
			"TN: " + measure.getTN() + "\n" +
			"FN: " + measure.getFN();
	}

	return measureData;
}
