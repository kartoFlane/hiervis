function() {
	// Load required classes
	var AdaptedFmeasure = Java.type( 'external_measures.AdaptedFmeasure' );

	// Initialize the measure object
	var withInstanceInheritance = false;
	var measure = new AdaptedFmeasure( withInstanceInheritance );

	// Create and return the result holder object
	var measureData = {};
	measureData.id = 'Adapted F-Measure (No Inheritance)';
	measureData.callback = function ( hierarchy ) {
		return measure.getMeasure( hierarchy );
	}

	return measureData;
}
