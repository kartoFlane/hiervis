function() {
	// Load required classes
	var InstancesPerLevel = Java.type( 'internal_measures.statistics.histogram.InstancesPerLevel' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new InstancesPerLevel();
	measureData.id = 'Instances Per Level';
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
