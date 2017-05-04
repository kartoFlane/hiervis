function() {
	// Load required classes
	var NumberOfNodes = Java.type( 'internal_measures.statistics.NumberOfNodes' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new NumberOfNodes();
	measureData.id = 'Number of Nodes';
	measureData.autoCompute = true;
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
