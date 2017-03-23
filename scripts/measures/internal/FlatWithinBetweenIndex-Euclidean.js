function() {
	// Load required classes
	var FlatWithinBetweenIndex = Java.type( 'internal_measures.FlatWithinBetweenIndex' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatWithinBetweenIndex( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Within-Between Index (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
