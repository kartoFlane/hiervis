function createMeasureData() {
	// Load required classes
	var FlatDaviesBouldin = Java.type( 'internal_measures.FlatDaviesBouldin' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatDaviesBouldin( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Davies-Bouldin (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
