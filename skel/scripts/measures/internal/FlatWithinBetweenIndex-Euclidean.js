function() {
	// Load required classes
	var FlatWithinBetweenIndex = Java.type( 'internal_measures.FlatWithinBetweenIndex' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new FlatWithinBetweenIndex( new Euclidean() );
	measureData.id = 'Flat Within-Between Index (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
