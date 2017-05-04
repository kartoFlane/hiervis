function() {
	// Load required classes
	var FlatDunn3 = Java.type( 'internal_measures.FlatDunn3' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new FlatDunn3( new Euclidean() );
	measureData.id = 'Flat Dunn 3 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
