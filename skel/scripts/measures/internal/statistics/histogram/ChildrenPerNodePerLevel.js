function() {
	// Load required classes
	var ChildPerNodePerLevel = Java.type( 'internal_measures.statistics.histogram.ChildPerNodePerLevel' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new ChildPerNodePerLevel();
	measureData.id = 'Children Per Node Per Level';
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
