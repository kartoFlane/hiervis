function() {
	// Load required classes
	var FlatInformationGain = Java.type( 'external_measures.information_based.FlatInformationGain' );
	var FlatEntropy1 = Java.type( 'external_measures.information_based.FlatEntropy1' );

	// Initialize the measure object
	var logBase = 2;
	var measure = new FlatInformationGain( logBase, new FlatEntropy1() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Information Gain (2, Flat1)';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
