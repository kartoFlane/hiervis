function createMeasureData() {
	// Load required classes
	var InstancesPerLevel = Java.type( 'internal_measures.statistics.histogram.InstancesPerLevel' );

	// Initialize the measure object
	var measure = new InstancesPerLevel();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Instances Per Level';
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
