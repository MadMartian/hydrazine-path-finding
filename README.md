# Hydrazine Path Finding Engine
This is a rather ~~basic~~ sophisticated, incrementally-progressive, single-threaded A* path-finding engine inspired by a certain blocky sandbox game that everyone has played before.  Due to its progressive and incremental A* approach this engine trades-off accuracy for significant boosts in path-finding performance by distributing A* graph computation across time and then rotating the graph on an as-needed basis as the subject entity physically traverses through path points derived from it.  

The tradeoff between accuracy and performance in this engine's algorithm is acceptable.  The inaccuracy means entities take slightly longer to arrive at their destinations, but this is actually more natural.  In the real world, creatures and people make mistakes finding their way from point A to point X.  Furthermore, it isn't even possible to instantly compute a perfect path from point A to point X through complex environments (this is precisely why maze puzzles are such a hit!).  Therefore this algorithm provides us with a much higher-performance and natural path-finding experience for computer-controlled entities.

Although this engine focuses on trading-off accuracy for performance, inaccurate path-finding is not always desirable.  This engine provides three APIs for computing paths of varying degree of accuracy at the expense of performance.  There is also a fourth API for computing 100% accurate traditional A* paths for such edge-cases that require them.  With these APIs a developer can take advantage of some of the other non-progressive performance benefits this engine offers without being forced to forego accurate path-finding.

## Featured in Mad Martian Mod
This engine was inspired by improving performance in another blocky sandbox game everyone is familiar with.  _Mad Martian Mod_ uses this engine and enjoys ~1000x boost in path-finding performance.

Check-out [Mad Martian Mod](https://gauss.extollit.com/minecraft/), download the launcher, play the mod, and see _Hydrazine_ in action.

There are also other projects that use Hydrazine Path-finding engine including the [Minestom](https://github.com/Minestom/Minestom) project, it has a large and active community.  You should definitely (literally?) check out this project if you're interested in customized server implementations.

## How to use this Engine
The engine works by leveraging interfaces wired-up to your game's implementation using the [dependency inversion principle](https://en.wikipedia.org/wiki/Dependency_inversion_principle) (see the types beneath the `model` package).
Then the primary entry-point for using the engine is `com.extollit.gaming.ai.path.HydrazinePathFinder`.  Here's a very basic example that assumes all the relevant interfaces tied to `myWorld` and `myEntity` have been implemented:

    final HydrazinePathFinder pathFinder = new HydrazinePathFinder(myEntity, myWorld);

    final IPath path = pathFinder.initiatePathTo(new Vec3d(1, 3.2, 5.8));

    if (path != null)
    do {
        path = pathFinder.updatePathFor(myEntity);
    } while (game.running());
    
### Selecting a Destination
For some approaches including random entity wandering (e.g. animals wandering around randomly) it is very uncommon for a
caller to select a target wander destination that is reachable (because this in-turn depends on path-finding, so you have
a chicken and egg paradox).  To remedy these situations one can make use of the `TargetingStrategy.gravitySnap`
strategy or the `TargetingStrategy.bestEffort` strategy located in `com.extollit.gaming.ai.path.PathOptions` (see the 
inline documentation there for details) as a means of responding to destinations that the path-finder cannot immediately 
confirm as reachable (e.g. a block up in the middle of the air).

Targeting strategies are defined using the `com.extollit.gaming.ai.path.PathOptions` object and passed to one of the 
`com.extollit.gaming.ai.path.HydrazinePathFinder` methods whose prototype takes such an object.  `PathOptions` also 
employs a builder pattern for method-chaining convenience:

    final HydrazinePathFinder pathFinder = new HydrazinePathFinder(myEntity, myWorld);
    final PathOptions pathOptions = new PathOptions()
        .targetingStrategy(PathOptions.TargetingStrategy.gravitySnap)

    final IPath path = pathFinder.initiatePathTo(4, 4.2, 8.1, pathOptions);

    if (path != null)
    do {
        path = pathFinder.updatePathFor(myEntity);
    } while (game.running());
    
It's a good idea to maintain static instances of `PathOptions` objects for performance reasons rather than construct a 
new instance for each path-finding operation.

### Path-finding Scheduling
There are four different ways to refine how much computing resources are allocated to a particular entity's path-finding
object through what is called a scheduling priority, namely `low`, `medium`, high` and `extreme`.  The higher the
scheduling priority the more fluid the path-finding operation at the expense of CPU utilization.  It is conventional to
set this priority according to the type of pathing entity upon construction time, although the priority can be changed 
at any time:

    // For animals
    final HydrazinePathFinder animalPathFinder = new HydrazinePathFinder(myCow, myWorld);
    animalPathFinder.schedulingPriority(SchedulingPriority.low)
    
    // For zombies
    final HydrazinePathFinder geekPathFinder = new HydrazinePathFinder(myGeek, myWorld);
    geekPathFinder.schedulingPriority(SchedulingPriority.high)
    
    // For intelligent assassin-like entities
    final HydrazinePathFinder maestroPathFinder = new HydrazinePathFinder(myMaestro, myWorld);
    maestroPathFinder.schedulingPriority(SchedulingPriority.extreme)

If you do not explicitly configure a path-finder with a scheduling priority then the default setting of `medium` is 
used.

### Compute Path (Non-Incremental)
For use-cases where it is most intuitive to compute a complete path from source position to destination all at once as
with most traditional A* path-finding algorithms use the `computePathTo` method:
    
    final HydrazinePathFinder pathFinder = new HydrazinePathFinder(myEntity, myWorld);

    final IPath path = pathFinder.computePathTo(new Vec3d(1, 3.2, 5.8));

    if (path == null)
        ; // Destination is definitely unreachable
    else
        path.update(myEntity);

**NOTE**: This method bypasses the incremental algorithm, which is designed to distribute path-finding effort over the 
lifetime of the path-traversal.  While this method guarantees a definitive and more accurate result it is exponentially 
more expensive than the incremental approach.

### Notifying the Engine of Changes
The path-finding engine must be notified of certain changes that occur to keep it up-to-date.  
This includes chunk loading / unloading and block changes, both which focus on the same conceptual chunk type.
The implementor must locate the associated `IColumnarSpace` object and interact with it accordingly.  If the 
implementor has opted to modify their own conceptual chunk type to implement `IColumnarSpace` (i.e. monist 
class topology) then this is trivial (see below).

#### Chunk Loading / Unloading
Just prior to loading or unloading a chunk, call `occlusionFields().reset()`.  The engine relies on the accessor functions
on `IColumnarSpace` and `IInstanceSpace` to initialize the `OcclusionField` objects:

    class MyChunkType implements IColumnarSpace {
        ...
        
        private final ColumnarOcclusionFieldList occlusionFieldList = new ColumnarOcclusionFieldList(this);
        
        ...
        
        public void load() {
            occlusionFields().reset();
            
            // Perform your chunk loading here
        }
        
        ...
        
        public final ColumnarOcclusionFieldList occlusionFields() { 
            return this.occlusionFieldList; 
        }
    }
    
#### Block Changes
After a block has been changed in the world (e.g. environment description, opening a door, building a house, etc.) the 
implementor must call `occlusionFields().onBlockChanged(...)` passing information about the new block:

    class MyBlock implements IBlockDescription {
        ...
    }
    
    class MyChunkType implements IColumnarSpace {
        ...
        
        private final ColumnarOcclusionFieldList occlusionFieldList = new ColumnarOcclusionFieldList(this);
        
        ...
        
        public void setBlock(int x, int y, int z, MyBlock block, int metaData) {
            // Perform your proprietary mutations here
            
            occlusionFields().onBlockChanged(x, y, z, block, metaData);
        }
        
        ...
        
        public final ColumnarOcclusionFieldList occlusionFields() { 
            return this.occlusionFieldList; 
        }
    }

## Dependencies
This module depends on *JUnit* but also on a utility library I wrote called *data-structures*, you can also find that on my GitHub channel.

# Examples
There are some skeletal example concrete implementations of the interfaces in the [examples](src/example/java/com/extollit/gaming/ai/path) directory demonstrating how to implement these interfaces. 
