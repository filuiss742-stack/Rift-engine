# Third-party notices

## libGDX integration spike

The isolated `gdx-spike` experiment uses the following Maven artifacts:

- `com.badlogicgames.gdx:gdx:1.14.2`
- `com.badlogicgames.gdx:gdx-backend-android:1.14.2`
- `com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2`
- `com.badlogicgames.gdx:gdx-platform:1.14.2` native classifiers

Project: https://github.com/libgdx/libgdx  
License: Apache License 2.0  
License text: https://www.apache.org/licenses/LICENSE-2.0  
Source license file: https://github.com/libgdx/libgdx/blob/master/LICENSE

The spike links against published libGDX artifacts; it does not copy libGDX source code into this repository. The project must include the Apache-2.0 license text and preserve applicable notices in any redistributed source or binary package that contains libGDX. Before a public game release, generate and review a complete notice inventory for every transitive dependency and native binary.

## Not yet included

Jolt Physics, Godot, and other candidates are research references only. They are not dependencies of the current project or the spike and must not be described as shipped components.
