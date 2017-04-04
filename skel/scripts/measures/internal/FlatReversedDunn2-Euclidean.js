function() {
	// Load required classes
	var FlatReversedDunn2 = Java.type( 'internal_measures.FlatReversedDunn2' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatReversedDunn2( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Reversed Dunn 2 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
