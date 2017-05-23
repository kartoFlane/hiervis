
Features
--------

1. Zooming in Instance Visualizations Frame

	Instance Visualizations frame features 3 different "zooming" techniques:
	1.1) Plain old zoom.
		Accessed by scrolling the mouse wheel / touchpad while holding down the Control key over the visualization you wish to zoom in.
	1.2) Resolution scaling, which increases the resolution at which the visualization is rendered. This is useful when you have many instances clustered together, to the point where it's impossible to distinguish them.
		Accessed by scrolling the mouse wheel / touchpad while holding down the Alt key over the visualization you wish to zoom in.
	1.3) Viewport scaling, which increases the size of visualizations' GUI components, so that they take up more area on the screen. It's useful for when you don't want to pan around the visualization too much.
		Accesed by scrolling the mouse wheel / touchpad while holding down the Control key over *EMPTY* area, where there is no visualization present.
		NOTE: visualizations "stretch" to fill all available area in the frame, therefore it's not possible to shrink them if they're already stretched.


Troubleshooting
---------------

1. Freeze of the entire desktop when opening dialog windows

	This happens on Unix systems running XFCE desktop environment with OpenJDK Java, when the application uses the default Swing theme: Metal.
	It seems to be a bug with either XFCE or OpenJDK implementation. As of the time of writing, the only way to prevent this from happening is to use a theme other than Metal. The application should do this automatically if it detects the aforementioned conditions.
	Should this bug ever be fixed, one can prevent the forced look-and-feel change by changing the 'stopXfceLafChange' property in config.json file to true.
