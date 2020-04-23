import java.awt.geom.*;
import java.awt.*;

/* An interface defining the behaviour of each entity in the game. The game
   engine keeps calling these methods as needed. */

public interface Entity {

    // Returns the Shape of this entity at time t, for collision detection purposes.
    public Shape getShape(int t);
    
    // Renders this entity at time t on the given Graphics2D object, the way that
    // it looks like that moment. Usually you getShape and fill that, but you can
    // render some other way also, for example as an Image.
    public void render(Graphics2D g2, int t);
    
    // The method that the game engine calls once for each time frame t. Write this
    // method so that it does what you want the entity to do at that time.
    public void action(int t);
    
    // Before calling action or render, the game engine calls this method. This
    // method should return true if the entity is still part of the game, and false
    // otherwise. Once this method returns false, the game engine will never call
    // either getShape, action or render again for this entity, and will lose all
    // references to this object.
    public boolean isActive();
    
    // Sends a String message to this entity. Write this method so that the entity
    // responds to the message in a way appropriate to the rules of this game.
    public void sendMessage(Entity source, String msg);
    
    // Returns the depth Z-coordinate for this entity. The game engine guarantees
    // that the entities are rendered in order that an entity with larger Z will
    // be rendered after all entities with lower Z. This method will be called
    // only once when the entity is added to the game, and thus cannot change
    // after that.
    public int getZ();
}
