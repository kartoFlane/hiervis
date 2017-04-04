function() {
	// Load required classes
	var HierarchicalInternalMeasure = Java.type( 'internal_measures.HierarchicalInternalMeasure' );
	var FlatWithinBetweenIndex = Java.type( 'internal_measures.FlatWithinBetweenIndex' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var qualityMeasure = new FlatWithinBetweenIndex( new Euclidean() );
	var measure = new HierarchicalInternalMeasure( qualityMeasure );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Hierarchical Internal Measure (Flat Within-Between Index, Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
