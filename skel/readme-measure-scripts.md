# Measure Scripts

The visualizer provides a generic mechanism that allows plugging in custom quality measures. This way, the user can add whatever quality measures that they find relevant in their line of work.

There are two ways to add new measures: either by including a compiled jar file with measure implementation, or implementing the measures directly in JavaScript in the script file.

## Option 1: Measures implemented in JavaScript

**This option is mostly relevant for simple measures that *do not* rely on external classes.**

First, go to [scripts/measures](scripts/measures) folder in the visualizer's directory, and create a new file, named `MyMeasure.js`. Open it with your preferred editor and insert the following code:

```
function() {
	var measureData = {};
	measureData.id = 'User-Friendly Name of My Measure';
	measureData.callback = function ( hierarchy ) {
		// implement your measure-calculating code here
	}

	return measureData;
}
```

And modify it accordingly by giving the measure a user-friendly name, and implementing your measure calculation in the `callback` function's body.


## Option 2: Measures implemented in Java

**This option is mostly relevant for complex measures that rely on external classes.**

Your Java code will have to use the following libraries: [basic_hierarchy](https://github.com/toSterr/basic_hierarchy) and [hierarchy_measures](https://github.com/toSterr/hierarchy_measures).

First, you will need to implement the measures you want in Java, using the [QualityMeasure](https://github.com/toSterr/hierarchy_measures/blob/master/src/interfaces/QualityMeasure.java) interface. Doing so allows the measures to define desired and undesired values, which are then shown in the GUI to inform the user.

If that functionality is not necessary, then technically any method that takes a [Hierarchy](https://github.com/toSterr/basic_hierarchy/blob/master/src/basic_hierarchy/interfaces/Hierarchy.java) object as argument and returns a numeric value, an array of doubles, or a string will be valid. In cases where a result in the form of an average with standard deviation is desired, the measure will need to return an instance of [AvgWithStdev](https://github.com/toSterr/hierarchy_measures/blob/master/src/internal_measures/statistics/AvgWithStdev.java) class.

Once the measures are implemented, you will need to export the project containing your measures as a jar file (it doesn't have to be runnable, just has to contain the compiled .class files). Place the jar file into [measure-jars](measure-jars) folder in the visualizer's directory.

The next part is very similar to Option 1, but with several key differences.

Go to [scripts/measures](scripts/measures) folder in the visualizer's directory, and create a new file, named `MyMeasure.js`. Open it with your preferred editor and insert the following code:

```
function() {
	// Import your measure class by inserting its fully-qualified name below
	// If your measure's constructor takes any specialized classes as argument,
	// you will need to import them in a similar fashion here.
	var MyMeasure = Java.type( 'package.path.to.MyMeasure' );
	var SomeSpecialArgument = Java.type( 'package.path.to.SomeSpecialArgument' );

	// Instantiate arguments for your measure
	// Assuming that 'MyMeasure' has a single-parameter constructor that takes
	// an instance of 'SomeSpecialArgument'
	var specialArg = new SomeSpecialArgument();

	var measureData = {};
	measureData.measure = new MyMeasure( specialArg );
	measureData.id = 'User-Friendly Name of My Measure';
	measureData.callback = function ( hierarchy ) {
		// Invoke the method that calculates the measure result
		// For measures implementing QualityMeasure interface, it's getMeasure()
		return this.measure.getMeasure( hierarchy );
	}

	return measureData;
}
```

Explanation:

- The first part of the file imports all classes necessary to create an instance of your measure class, and saves references to them.
- Next, arguments for your measure's constructor are instantiated, by invoking constructors via the references we saved earlier.
- Then we create a wrapper object that holds all data pertaining to your measure. Here we also create an instance of your measure and store it in the wrapper object.

It is possible to make the visualizer compute the measure automatically as soon as a hierarchy is loaded. To do that, define a new field in the wrapper object named `autoCompute` and set it to `true`, like so:

```
measureData.autoCompute = true;
```

In cases where your measure is very specialized and requires the hierarchy to fulfill some criteria, you can define an `isApplicable` field in the wrapper object and set it to a function that evaluates whether the hierarchy can be applied to the measure or not. Example:

```
measureData.isApplicable = function ( hierarchy ) {
	// Applicable only to hierarchies with ground truth attribute
	return hierarchy.getNumberOfClasses() > 0;
}
```

Such a measure will only show up in the visualizer's GUI if the `isApplicable` function returns `true` for the currently loaded hierarchy.
