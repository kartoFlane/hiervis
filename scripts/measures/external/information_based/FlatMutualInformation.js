function() {
	// Load required classes
	var FlatMutualInformation = Java.type( 'external_measures.information_based.FlatMutualInformation' );

	// Initialize the measure object
	var measure = new FlatMutualInformation();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Mutual Information';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
