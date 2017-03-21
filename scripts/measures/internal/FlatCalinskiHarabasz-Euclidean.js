function createMeasureData() {
	// Load required classes
	var FlatCalinskiHarabasz = Java.type( 'internal_measures.FlatCalinskiHarabasz' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var measure = new FlatCalinskiHarabasz( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Flat Calinski-Harabasz (Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
