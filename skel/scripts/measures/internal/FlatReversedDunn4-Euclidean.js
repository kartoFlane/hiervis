function() {
	// Load required classes
	var FlatReversedDunn4 = Java.type( 'internal_measures.FlatReversedDunn4' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatReversedDunn4( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Reversed Dunn 4 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
