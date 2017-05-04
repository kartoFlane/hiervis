function() {
	// Load required classes
	var FlatReversedDunn4 = Java.type( 'internal_measures.FlatReversedDunn4' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new FlatReversedDunn4( new Euclidean() );
	measureData.id = 'Flat Reversed Dunn 4 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
