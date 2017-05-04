function() {
	// Load required classes
	var FlatEntropy1 = Java.type( 'external_measures.information_based.FlatEntropy1' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new FlatEntropy1();
	measureData.id = 'Flat Entropy 1';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
