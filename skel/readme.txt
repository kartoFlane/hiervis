
Troubleshooting
---------------

1. Freeze of the entire desktop when opening dialog windows

	This happens on Unix systems running XFCE desktop environment with OpenJDK Java, when the application uses the default Swing theme: Metal.
	It seems to be a bug with either XFCE or OpenJDK implementation. As of the time of writing, the only way to prevent this from happening is to use a theme other than Metal. The application should do this automatically if it detects the aforementioned conditions.
	Should this bug ever be fixed, one can prevent the forced look-and-feel change by changing the 'stopXfceLafChange' property in config.json file to true.
