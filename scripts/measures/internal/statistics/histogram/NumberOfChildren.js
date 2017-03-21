function createMeasureData() {
	// Load required classes
	var HistogramOfNumberOfChildren = Java.type( 'internal_measures.statistics.histogram.HistogramOfNumberOfChildren' );

	// Initialize the measure object
	var measure = new HistogramOfNumberOfChildren();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Number Of Children';
	measureData.callback = function ( hierarchy ) {
		return measure.calculate( hierarchy );
	}

	return measureData;
}
