function() {
	// Load required classes
	var HierarchicalClassPurity = Java.type( 'external_measures.purity.HierarchicalClassPurity' );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new HierarchicalClassPurity();
	measureData.id = 'Hierarchical Class Purity';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
