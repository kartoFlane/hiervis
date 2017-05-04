function() {
	// Load required classes
	var FlatDunn1 = Java.type( 'internal_measures.FlatDunn1' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new FlatDunn1( new Euclidean() );
	measureData.id = 'Flat Dunn 1 (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
