function createMeasureData() {
	// Load required classes
	var FlatDunn4 = Java.type( 'internal_measures.FlatDunn4' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatDunn4( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Dunn 4 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
