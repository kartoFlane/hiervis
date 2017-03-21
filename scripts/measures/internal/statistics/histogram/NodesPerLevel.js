function createMeasureData() {
	// Load required classes
	var NodesPerLevel = Java.type( 'internal_measures.statistics.histogram.NodesPerLevel' );

	// Initialize the measure object
	var measure = new NodesPerLevel();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Nodes Per Level';
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
