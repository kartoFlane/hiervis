function() {
	// Load required classes
	var HierarchicalClassPurity = Java.type( 'external_measures.purity.HierarchicalClassPurity' );

	// Initialize the measure object
	var measure = new HierarchicalClassPurity();

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Hierarchical Class Purity';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
