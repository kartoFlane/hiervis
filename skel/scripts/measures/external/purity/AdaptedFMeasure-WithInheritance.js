function() {
	// Load required classes
	var AdaptedFmeasure = Java.type( 'external_measures.AdaptedFmeasure' );

	var withInstanceInheritance = true;

	// Create and return the result holder object
	var measureData = {};
	measureData.measure = new AdaptedFmeasure( withInstanceInheritance );
	measureData.id = 'Hierarchical F-Measure (With Inheritance)';
	measureData.isApplicable = function ( hierarchy ) {
		// Applicable only to hierarchies with ground truth attribute
		return hierarchy.getNumberOfClasses() > 0;
	}
	measureData.callback = function ( hierarchy ) {
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
