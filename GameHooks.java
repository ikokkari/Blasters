import java.util.*;
import java.awt.geom.*;
import javax.swing.*;

/* The methods that the game engine offers to the levels and entities of the game.
 * They can affect the game by calling these methods.
 */

public interface GameHooks {

    // Grant the player some points for something good that he did in the game.
    public void grantPoints(int points);
    
    // Add a new active entity to the game. The entity will be part of the game
    // from the next time frame, and will receive render and action calls for 
    // each time frame from then on.
    public void addEntity(Entity e);
    
    // Note that there is no corresponding method removeEntity, since it might
    // lead to concurrent modification errors. Instead, the game engine will
    // periodically query each entity of whether it is still active, and if it
    // answers no, remove that entity from the list of active entities.
    
    // Returns the list of entities whose shapes intersect the entity e at this
    // time. The method can be given a Rectangle2D.Double bounding box that is
    // either null or completely surrounds the entity, to speed up intersection
    // detection. Only entities with the same Z-level count for collisions.
    public List<Entity> getCollisions(Entity e, Rectangle2D.Double boundingBox);
    
    // Returns the Swing component that the game engine uses to display the
    // game. Your levels and entities can then add various event listeners to
    // that component to react to user interaction.
    public JComponent getComponent();
    
    // The game engine can display a short String message hovering on top of
    // user mouse cursor. Use this method to set that message and the time
    // (measured in frames) how long that message is displayed.
    public void setMessage(String message, int delay);
    
}
