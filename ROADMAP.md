# Roadmap
This file outlines the purpose of each folder in the repository.

## lib
This folder contains all libraries used by the program. Linking them in your project as-is should allow the source code to compile successfully.

## skel
This folder contains startup batch/shell scripts that aid in starting the program correctly.

The start.sh shell script is not my own invention. It's been created (or otherwise found / conjured) by David 'Vhati' Millis.

## src
Unsurprisingly, this folder contains the source code of the program.

## test
This directory contains tests for the software's source code. Separated from the `src` folder to prevent inclusion of JUnit testing suite as a dependency.
