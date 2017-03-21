function createMeasureData() {
	// Load required classes
	var FlatEntropy1 = Java.type( 'external_measures.information_based.FlatEntropy1' );

	// Initialize the measure object
	var measure = new FlatEntropy1();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Entropy 1';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
