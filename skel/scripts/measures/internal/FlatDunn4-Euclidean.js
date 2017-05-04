function() {
	// Load required classes
	var FlatDunn4 = Java.type( 'internal_measures.FlatDunn4' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new FlatDunn4( new Euclidean() );
	measureData.id = 'Flat Dunn 4 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
