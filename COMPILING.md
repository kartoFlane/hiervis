# Compiling

The source code for this sofware makes heavy use of lambas, and as such will require Java 8 to compile.

Assuming that no changes need to be made to any of the projects on which this software depends, compilation consists only of cloning the repository and running the [Ant build file](ant_build.xml) (in [Eclipse](https://www.eclipse.org/): right click on the Ant build file, select `Run As... > Ant Build`).

In case such changes are necessary, the projects in question will need to be updated, recompiled, and their respective jars in the [libraries](lib) folder replaced with the new versions. After that, the build file will need to be ran again to rebuild the visualizer.

## Dependencies

All of the dependencies listed here are included for convenience in the [libraries](lib) folder.

This software relies on the following projects, and requires them to run:

- [Basic Hierarchy](https://github.com/toSterr/basic_hierarchy/)
- [Hierarchy Measures](https://github.com/toSterr/hierarchy_measures/)
- [HK++](https://github.com/toSterr/hkplusplus/)
	- This project's jar was modified to use the same lib folder as the rest of the program, to prevent library redundancy.

This software also uses the following libraries:

- [Prefuse](http://prefuse.org/), version beta-20071021
- [Apache Commons](https://commons.apache.org/)
    - [CLI](https://commons.apache.org/proper/commons-cli/), version 1.2
    - [Lang](https://commons.apache.org/proper/commons-lang/), version 3.4
    - [Math](https://commons.apache.org/proper/commons-math/), version 3.6
- [Apache Log4j](http://logging.apache.org/log4j/2.x/), version 2.5
- [Jackson](https://github.com/FasterXML/jackson), version 2.4.1
