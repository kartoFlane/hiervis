function() {
	// Load required classes
	var LeavesPerLevel = Java.type( 'internal_measures.statistics.histogram.LeavesPerLevel' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new LeavesPerLevel();
	measureData.id = 'Leaves Per Level';
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
