function createMeasureData() {
	// Load required classes
	var FlatDunn1 = Java.type( 'internal_measures.FlatDunn1' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatDunn1( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Dunn 1 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
