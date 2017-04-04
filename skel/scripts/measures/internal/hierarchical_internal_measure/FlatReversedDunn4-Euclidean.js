function() {
	// Load required classes
	var HierarchicalInternalMeasure = Java.type( 'internal_measures.HierarchicalInternalMeasure' );
	var FlatReversedDunn4 = Java.type( 'internal_measures.FlatReversedDunn4' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var qualityMeasure = new FlatReversedDunn4( new Euclidean() );
	var measure = new HierarchicalInternalMeasure( qualityMeasure );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Hierarchical Internal Measure (Flat Reversed Dunn 4, Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
