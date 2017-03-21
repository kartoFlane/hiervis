function createMeasureData() {
	// Load required classes
	var FlatDunn2 = Java.type( 'internal_measures.FlatDunn2' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatDunn2( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Dunn 2 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
