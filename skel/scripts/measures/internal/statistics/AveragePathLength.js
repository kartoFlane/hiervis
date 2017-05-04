function() {
	// Load required classes
	var AvgPathLength = Java.type( 'internal_measures.statistics.AvgPathLength' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new AvgPathLength();
	measureData.id = 'Average Path Length';
	measureData.autoCompute = true;
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
