/* A specialization of NewtonEntity so that the object can be given animation motion path
 * as a Bezier curve that starts from the current location (getX(), getY()) and ends at
 * target (p3x, p3y) so that the shape of the path meanwhile is determined by two control
 * points (p1x, p1y) and (p2x, p2y) that the path does not in general intersect. As a special
 * case of cubic Bezier curve, a linear path convenience method is provided.
 */

public abstract class BezierEntity extends NewtonEntity {

    private int ts, te;
    private double ax, bx, cx, ay, by, cy, p0x, p0y;
    private boolean stopAtEnd;
    
    // Calling this method makes the entity stop once it reaches the end of path.
    public void setStopAtEnd(boolean stopAtEnd) {
        this.stopAtEnd = stopAtEnd;
    }
    
    // Set the movement path as Bezier curve.
    public void setBezierPath(double p1x, double p1y, double p2x, double p2y, double p3x, double p3y, int ts, int te) {
        this.ts = ts;
        this.te = te;
        p0x = getX();
        cx = 3 * (p1x - p0x);
        bx = 3 * (p2x - p1x) - cx;
        ax = p3x - p0x - cx - bx;
        p0y = getY();
        cy = 3 * (p1y - p0y);
        by = 3 * (p2y - p1y) - cy;
        ay = p3y - p0y - cy - by;  
    }
    
    // Set the movement path as linear path.
    public void setLinearPath(double p3x, double p3y, int ts, int te) {
        double p1x = p3x / 3 + 2 * getX() / 3;
        double p1y = p3y / 3 + 2 * getY() / 3;
        double p2x = 2 * p3x / 3 + getX() / 3;
        double p2y = 2 * p3y / 3 + getY() / 3;
        setBezierPath(p1x, p1y, p2x, p2y, p3x, p3y, ts, te);
    }
    
    public void action(int t) {
        if(ts <= t && t < te) {
            double t2 = (t + 1 - ts) / (double)(te - ts);
            double nextX = ((((ax * t2) + bx) * t2) + cx) * t2 + p0x;
            double nextY = ((((ay * t2) + by) * t2) + cy) * t2 + p0y;
            setVX(nextX - getX());
            setVY(nextY - getY());
        }
        if(t == te && stopAtEnd) { holdStill(); }
        super.action(t);
    }
    
}
