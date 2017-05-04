function() {
	// Load required classes
	var HierarchicalInternalMeasure = Java.type( 'internal_measures.HierarchicalInternalMeasure' );
	var FlatDaviesBouldin = Java.type( 'internal_measures.FlatDaviesBouldin' );
	var Euclidean = Java.type( 'distance_measures.Euclidean' );

	// Initialize the measure object
	var qualityMeasure = new FlatDaviesBouldin( new Euclidean() );

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new HierarchicalInternalMeasure( qualityMeasure );
	measureData.id = 'Hierarchical Internal Measure (Flat Davies-Bouldin, Euclidean)';
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
