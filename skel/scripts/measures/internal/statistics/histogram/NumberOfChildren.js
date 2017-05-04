function() {
	// Load required classes
	var HistogramOfNumberOfChildren = Java.type( 'internal_measures.statistics.histogram.HistogramOfNumberOfChildren' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new HistogramOfNumberOfChildren();
	measureData.id = 'Number Of Children';
	measureData.callback = function ( hierarchy ) {
		return this.measure.calculate( hierarchy );
	}

	return measureData;
}
