/* The methods that each level of the game must implement. */

public interface Level {

    // A method that will be called once when the level begins. Write this method
    // so that it creates the initial entities of the level.
    public void initialize(int t);
    
    // A method that the game engine uses to query whether this level is complete.
    // If this method returns true, the engine moves on to the next level.
    public boolean isCompleted(int t);
    
    // A method that the game engine calls once for each time frame t. Write this
    // method to create the new entities of that time and enforce the rules of the
    // game.
    public void action(int t);
    
}
