function() {
	// Load required classes
	var Height = Java.type( 'internal_measures.statistics.Height' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new Height();
	measureData.id = 'Height';
	measureData.autoCompute = true;
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
