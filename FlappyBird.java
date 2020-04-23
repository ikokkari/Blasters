import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.geom.*;
import javax.swing.*;

public class FlappyBird implements Game {
   
    // Pixel dimensions of the game.
    private static final int WIDTH = 700, HEIGHT = 500;
    // How often (measured in frames) a new pipe entity is created in the game.
    private static final int PIPEFREQUENCY = 150;
    // The pixel gap between the upper and lower pipe.
    private static final double PIPEGAP = 100;
    // The pixel width of the pipe.
    private static final double PIPEWIDTH = 50;
    // The radius of Flappy in pixels.
    private static final double FR = 20;
    // Whether Flappy is still alive (even dead Flappy is still animated in the game)
    private boolean isAlive;
    // The hooks for the game engine.
    private GameHooks hooks;
    // The random number generator used in the game.
    private Random rng = new Random();
    // The graphical reprsentation of the game.
    private Image flappyI;
    
    public String getTitle() { return "Flappy Bird Space"; }
    public String getAuthor() { return "Ilkka Kokkarinen"; }
    
    // The constructor of the game should load all images and other resources used by the game.
    public FlappyBird() {
        flappyI = Toolkit.getDefaultToolkit().getImage("flappy.png");
        MediaTracker m = new MediaTracker(new JPanel());
        m.addImage(flappyI, 0);
        try { m.waitForAll(); } catch(InterruptedException e) { }
        flappyI = flappyI.getScaledInstance((int)(2*FR), (int)(2*FR), Image.SCALE_AREA_AVERAGING);    
    }
    
    // When starting a new game, create the initial entities to the game, and the list of
    // levels in this game. This game has only one level, but there could be more.
    public java.util.List<Level> startNewGame(GameHooks hooks) {        
        this.hooks = hooks;
        hooks.addEntity(new Flappy());
        isAlive = true;
        Level[] levels = { new FlappyLevel() };
        return Arrays.asList(levels);
    }
    
    // Return the pixel dimension of this game.
    public Dimension getDimension() {
        return new Dimension(WIDTH, HEIGHT);
    }
    
    // As this game holds no threads or other resources, this method can be empty. Otherwise,
    // we would release all that stuff here.
    public void terminate() { }
    
    // A nested class to implement the functionality of the level that Flappy keeps flying in.
    private class FlappyLevel implements Level {
        
        // A helper method to create a new pipe consisting of two pieces.
        private void addPipe() {
            double y = 50 + rng.nextDouble() * (HEIGHT - 200);
            hooks.addEntity(new Pipe(WIDTH + 1, 0, PIPEWIDTH, y));
            hooks.addEntity(new Pipe(WIDTH + 1, y + PIPEGAP, PIPEWIDTH, HEIGHT - (PIPEGAP + y)));    
        }
        
        // Since the game already created Flappy, we just need to create the first pipe.
        public void initialize(int t) {
            addPipe();
        }
        
        // Poor Flappy, doomed to fly forever.
        public boolean isCompleted(int t) { return false; }
        
        // Depending on current time, add a new pipe or a decorative background star to the game.
        public void action(int t) {
            if(t % PIPEFREQUENCY == 0) {
                addPipe();
            }
            if(t % 10 == 0) {
                hooks.addEntity(new Star());
            }
        } 
    }
    
    // The nested class for Flappy.
    private class Flappy extends NewtonEntity {
        // Is the mouse button currently down?
        private boolean mouseDown;
 
        // Since Flappy is a NewtonEntity, we can easily set his position. In this game, Flappy
        // remains in one x-coordinate while the pipes come at him.
        public Flappy() {
            this.setX(50);
            this.setY(HEIGHT / 2);
            hooks.getComponent().addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent me) {
                    mouseDown = true;
                }
                public void mouseReleased(MouseEvent me) {
                    mouseDown = false;
                }
            });
        }
        
        // Flappy's shape for collision detection purposes.
        public Shape getShape(int t) {
            return new Ellipse2D.Double(getX() - FR, getY() - FR, 2*FR, 2*FR);
        }
        
        // Render Flappy on the game.
        public void render(Graphics2D g2, int t) {
            g2.drawImage(flappyI, (int)(getX() - FR), (int)(getY() - FR), hooks.getComponent());
        }
        
        // At each time Frame, Flappy's y-acceleration is affected by gravity, and if he is dead,
        // he will generate a decorative Spark.
        public void action(int t) {
            setAY(0.1);
            if(!isAlive) {
                hooks.addEntity(new Spark(getX(), getY()));
            }
            else {
                if(mouseDown) { setAY(-0.35); }
                java.util.List<Entity> collisions = hooks.getCollisions(this, null);
                if(collisions.size() > 0) {
                    isAlive = false; hooks.setMessage("Flappy burst his space helmet!", 25);
                }
            }
            super.action(t);
            // Make it so that Flappy can't go outside game height bounds.
            if(getY() < 5) { setY(5); setVY(-getVY()); }
            if(getY() - 5 > HEIGHT) { setVY(-1); }
            
        }
        // Dead or alive, Flappy will remain in the game.
        public boolean isActive() { return true; }
        public void sendMessage(Entity source, String msg) { }
        public int getZ() { return 1; }
    }
    
    // The nested class for individual pipes. In addition to the position (x,y) of its top
    // left corner, each pipe has width w and height h. Each pipe moves left with constant
    // velocity of -1.7.
    private static final Color PIPECOLOR = new Color(0, 255, 0, 125);
    private class Pipe extends NewtonEntity {
        private double w, h;
        private boolean pointsGranted = false;
        public Pipe(double x, double y, double w, double h) {
            this.setX(x); this.setY(y); this.w = w; this.h = h;
            this.setVX(-1.7);
        }
        
        public Shape getShape(int t) {
            return new Rectangle2D.Double(getX(), getY(), w, h);
        }
        public void render(Graphics2D g2, int t) {
            g2.setPaint(PIPECOLOR);
            g2.fill(this.getShape(t));
            g2.setPaint(Color.WHITE);
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(this.getShape(t));
        }
        
        // Since Flappy already handles the collisions, we don't need to do that here, and
        // since super.action(t) moves the pipe, all we need to do is grant points once Flappy
        // gets through this pipe.
        public void action(int t) {
            super.action(t);
            double x = getX() + w;
            if(isAlive && !pointsGranted && getX() + w < 50) {
                hooks.grantPoints(5);
                pointsGranted = true;
            }
        }
        
        // Once the pipe goes past the left edge, it is removed from the game.
        public boolean isActive() {
            return getX() + w > 0;
        }
        public void sendMessage(Entity source, String msg) { }
        public int getZ() { return 1; }
    }
    
    // A nested class for the decorative stars in the background. Since their Z-level is
    // lower than Flappy's, the collisions don't count. Each star begins at the right edge
    // and a random shade, and for a primitive 3D effect, the dimmer the star, the slower
    // it moves.
    private class Star extends NewtonEntity {
        private Color color;
        public Star() {
            this.setX(WIDTH);
            this.setY(rng.nextDouble() * HEIGHT);
            int shade = rng.nextInt(255);
            this.color = new Color(shade, shade, shade);
            this.setVX(-shade / 100.0);
        }

        public Shape getShape(int t) {
            return new Rectangle2D.Double(getX(), getY(), 3, 3);
        }

        public void render(Graphics2D g2, int t) {
            g2.setPaint(color);
            g2.fill(this.getShape(t));
        }

        public void action(int t) {
            super.action(t);
        }

        // Just like pipes, stars cease to exist once they move past the left edge.
        public boolean isActive() {
            return getX() > 0;
        }
    }
    
    // A nested class for the sparks that dead Flappy sputters all around him.
    private class Spark extends NewtonEntity {
        private Color color;
        public Spark(double x, double y) {
            // Choose a random angle and set the position and velocity accordingly.
            double angle = rng.nextDouble() * 2 * Math.PI;
            this.setX(x + FR * Math.sin(angle));
            this.setY(y + FR * Math.cos(angle));
            this.setVX(10 * Math.sin(angle));
            this.setVY(10 * Math.cos(angle));
            // Sparks are affected by gravity.
            this.setAY(0.12);
            this.color = new Color(rng.nextInt(100) + 150, 0, 0);
        }

        public Shape getShape(int t) {
            return new Rectangle2D.Double(getX(), getY(), 3, 3);
        }

        public void render(Graphics2D g2, int t) {
            g2.setPaint(color);
            g2.fill(this.getShape(t));
        }

        public void action(int t) {
            super.action(t);
        }
        
        // Since the sparks can fly to any direction, we need to check if they have
        // gone outside any edge.
        public boolean isActive() {
            return getX() > 0 && getX() < WIDTH && getY() > 0 && getY() < HEIGHT;
        }
    }
    
    public static void main(String[] args) {
        final JFrame f = new JFrame("Flappy Bird Space!");
        final GameEngine blast = new GameEngine(new FlappyBird());
        f.add(blast);
        // Must ensure that timer is stopped when the Frame closes
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                blast.terminate();
                f.dispose();
            }
        });
        f.pack();
        f.setVisible(true);        
    }
}
