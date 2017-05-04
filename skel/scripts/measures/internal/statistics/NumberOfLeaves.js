function() {
	// Load required classes
	var NumberOfLeaves = Java.type( 'internal_measures.statistics.NumberOfLeaves' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new NumberOfLeaves();
	measureData.id = 'Number of Leaves';
	measureData.autoCompute = true;
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
