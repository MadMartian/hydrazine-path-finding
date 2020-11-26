# Hydrazine Path Finding Engine
This is a rather basic A* path-finding engine inspired by a certain blocky sandbox game that everyone has played before.

## How to use this Engine
The engine works by leveraging interfaces wired-up to your game's implementation using the [dependency inversion principle](https://en.wikipedia.org/wiki/Dependency_inversion_principle) (see the types beneath the `model` package).
Then the primary entry-point for using the engine is `com.extollit.gaming.ai.path.HydrazinePathFinder`.  Here's a very basic example that assumes all the relevant interfaces tied to `myWorld` and `myEntity` have been implemented:

    final HydrazinePathFinder pathFinder = new HydrazinePathFinder(myEntity, myWorld);

    final IPath path = pathFinder.initiatePathTo(new Vec3d(1, 3.2, 5.8));

    if (path != null)
    do {
        path = pathFinder.updatePathFor(myEntity);
    } while (game.running());
    
### Path-finding Scheduling
There are three different ways to refine how much computing resources are allocated to a particular entity's path-finding
object through what is called a scheduling priority, namely `low`, `high` and `extreme`.  The higher the scheduling priority
the more fluid the path-finding operation at the expense of CPU utilization.  It is conventional to set this priority 
according to the type of pathing entity upon construction time, although the priority can be changed at any time:

    // For animals
    final HydrazinePathFinder animalPathFinder = new HydrazinePathFinder(myCow, myWorld);
    animalPathFinder.schedulingPriority(SchedulingPriority.low)
    
    // For zombies
    final HydrazinePathFinder geekPathFinder = new HydrazinePathFinder(myGeek, myWorld);
    geekPathFinder.schedulingPriority(SchedulingPriority.high)
    
    // For intelligent assassin-like entities
    final HydrazinePathFinder maestroPathFinder = new HydrazinePathFinder(myMaestro, myWorld);
    maestroPathFinder.schedulingPriority(SchedulingPriority.extreme)


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
