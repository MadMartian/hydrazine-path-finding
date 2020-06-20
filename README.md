# Hydrazine Path Finding Engine
This is a rather basic A* path-finding engine inspired by a certain blocky sandbox game that everyone has played before.

## How to use this Engine
The engine works by leveraging interfaces wired-up to your game's implementation using the [dependency inversion principle](https://en.wikipedia.org/wiki/Dependency_inversion_principle) (see the types beneath the `model` package).
Then the primary entry-point for using the engine is `com.extollit.gaming.ai.path.HydrazinePathFinder`.  Here's a very basic example that assumes all the relevant interfaces tied to `myWorld` and `myEntity` have been implemented:

        final HydrazinePathFinder pathFinder = new HydrazinePathFinder(myEntity, myWorld);

        final PathObject path = pathFinder.initiatePathTo(new Vec3d(1, 3.2, 5.8));

        if (path != null)
        do {
            path.update(myEntity);
        } while (game.running());            

## Dependencies
This module depends on ''JUnit'' but also on a utility library I wrote called ''data-structures'', you can also find that on my GitHub channel.

# Examples
There are some skeletal example concrete implementations of the interfaces beneath the `src/example/java` directory demonstrating
how to implement these interfaces. 
