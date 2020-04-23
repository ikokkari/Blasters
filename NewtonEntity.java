/* A useful abstract class that implements an Entity that has physical
 * (x,y) location, (vx,vy) velocity and (ax,ay) acceleration, with getters
 * and setters for all six quantities. The action method moves this entity
 * according to Newtonian motion equations. When extending this class,
 * remember to call super.action(t) in your action method.
 */

public abstract class NewtonEntity implements Entity {
    
    private double x, y, vx, vy, ax, ay;
    
    public Entity setX(double x) { this.x = x; return this; }
    public Entity setY(double y) { this.y = y; return this; }
    public Entity setVX(double vx) { this.vx = vx; return this; }
    public Entity setVY(double vy) { this.vy = vy; return this; }
    public Entity setAX(double ax) { this.ax = ax; return this; }
    public Entity setAY(double ay) { this.ay = ay; return this; }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVX() { return vx; }
    public double getVY() { return vy; }
    public double getAX() { return ax; }
    public double getAY() { return ay; }
        
    public void action(int t) {
        vx += ax;
        vy += ay;
        x += vx;
        y += vy;
    }
    
    public void holdStill() {
        vx = vy = ax = ay = 0;
    }
    
    /* Might as well. */
    public void sendMessage(Entity source, String msg) { }
    public int getZ() { return 0; }
}
