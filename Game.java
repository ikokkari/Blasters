import java.util.*;
import java.awt.*;

/* An interface defining the methods that each entire Game must implement. */

public interface Game {

    // The title and the author of the game.
    public String getTitle();
    public String getAuthor();
    
    // The initialization method of this game. Must return a list of levels
    // that must be nonempty.
    public java.util.List<Level> startNewGame(GameHooks hooks);
    
    // The size of this game on the screen.
    public Dimension getDimension();
    
    // A method that the game engine calls to shut down this game. This method
    // should release all resources and terminate all threads held by the game.
    public void terminate();
    
}
