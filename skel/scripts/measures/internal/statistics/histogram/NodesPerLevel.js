function() {
	// Load required classes
	var NodesPerLevel = Java.type( 'internal_measures.statistics.histogram.NodesPerLevel' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new NodesPerLevel();
	measureData.id = 'Nodes Per Level';
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
