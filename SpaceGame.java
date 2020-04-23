import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

public class SpaceGame implements Game {

    private final int WIDTH = 500, HEIGHT = 800;
    private GameHooks hooks;
    private static final Random rng = new Random();

    public String getTitle() { return "Space Blasters"; }
    public String getAuthor() { return "Ilkka Kokkarinen"; }
    
    public java.util.List<Level> startNewGame(GameHooks hooks) {
        this.hooks = hooks;
        ArrayList<Level> levels = new ArrayList<Level>();
        levels.add(new CentipedeLevel());
        levels.add(new SpaceInvadersLevel());
        levels.add(new BallsLevel());
        levels.add(new SierpinskiLevel());        
        hooks.addEntity(new SpacePlayer());
        return levels;
    }

    public Dimension getDimension() {
        return new Dimension(WIDTH, HEIGHT);
    }

    public void terminate() { }
    
    private class SierpinskiLevel implements Level {
        private int startTime;
        public void initialize(int t) {
            hooks.setMessage("Break down the Sierpinski triangle!", 25);
            this.startTime = t;
            hooks.addEntity(new Sierpinski(10, -(WIDTH - 20), WIDTH - 20, WIDTH - 20)); 
        }

        public boolean isCompleted(int t) {
            return t - startTime > 0.75 * HEIGHT;
        }

        public void action(int t) {
            if(rng.nextInt(100) < 10) {
                hooks.addEntity(new Star());
            }
        }    
    }
    
    private static final int SIROWS = 6, SICOLS = 10;
    private static final double[][] SIDIRS = { { 2, 0 }, { 0, 2 }, { -2, 0 }, { 0, 2 } };
    private class SpaceInvadersLevel implements Level {
        private Image si;
        public SpaceInvadersLevel() {
            si = Toolkit.getDefaultToolkit().getImage("spaceinvader.jpeg");
            MediaTracker m = new MediaTracker(hooks.getComponent());
            m.addImage(si, 0);
            try { m.waitForAll(); } catch(InterruptedException e) { }
            si = si.getScaledInstance(15, 15, Image.SCALE_AREA_AVERAGING);
        }
        
        private int alienCount, sidir, downtime;
        private double maxX, minX, speedMultiplier;
        public void initialize(int t) {
            hooks.setMessage("Space Invaders!", 25);
            alienCount = downtime = sidir = 0;
            minX = 25; maxX = 25 + (SICOLS - 1) * (WIDTH - 150) / SICOLS; 
            for(int row = 0; row < SIROWS; row++) {
                for(int col = 0; col < SICOLS; col++) {
                    hooks.addEntity(new SpaceInvader(25 + col * (WIDTH - 150) / SICOLS, 40 + 30 * row));
                    alienCount++;
                }
            }
            speedMultiplier = 1;
        }

        public boolean isCompleted(int t) {
            return alienCount < 1;
        }

        public void action(int t) {
            if(alienCount == SIROWS * SICOLS / 2) { speedMultiplier = 1.2; }
            if(alienCount == SIROWS * SICOLS / 4) { speedMultiplier = 1.5; }
            if(alienCount == SIROWS * SICOLS / 5) { speedMultiplier = 2.0; }
            if(alienCount == 3) { speedMultiplier = 2.5; }
            if(alienCount == 2) { speedMultiplier = 3.0; }
            if(alienCount == 1) { speedMultiplier = 5; }
            if(sidir == 1 && t - downtime > 10) { sidir = 2; }
            else if(sidir == 0 && maxX > WIDTH - 20) { sidir = 1; downtime = t; }
            else if(sidir == 3 && t - downtime > 10) { sidir = 0; }
            else if(sidir == 2 && minX < 20) { sidir = 3; downtime = t; }
            if(rng.nextInt(100) < 10) {
                hooks.addEntity(new Star());
            }
            maxX = 0; minX = WIDTH;
        }   
        
        private class SpaceInvader extends NewtonEntity {
            private boolean isAlive = true;

            public SpaceInvader(double x, double y) {
                this.setX(x); this.setY(y);
            }
        
            public Shape getShape(int t) {
                return new Rectangle2D.Double(getX(), getY(), 15, 15);
            }

            public void render(Graphics2D g2, int t) {
                g2.drawImage(si, (int)getX(), (int)getY(), null);
            }

            public void action(int t) {
                this.setVX(SIDIRS[sidir][0] * speedMultiplier);
                this.setVY(SIDIRS[sidir][1]);
                super.action(t);
                if(getX() > maxX) { maxX = getX(); }
                if(getX() < minX) { minX = getX(); }
            }

            public boolean isActive() { 
                if(isAlive && getY() > HEIGHT) {
                    isAlive = false; alienCount--;
                }
                return isAlive;
            }

            public void sendMessage(Entity source, String msg) {
                if(msg.equals("Die!")) {
                    this.isAlive = false;
                    alienCount--;
                    hooks.addEntity(new Explosion(getX() + 7, getY() + 7));
                }
                source.sendMessage(this, "Die!");
            }

            public int getZ() { return 2; }
        }
        
    }
    
    private class BallsLevel implements Level {        
        private int startTime;

        public void initialize(int t) {
            hooks.setMessage("Spinning balls!", 25);
            this.startTime = t;
        }

        public boolean isCompleted(int t) {
            return t - startTime > 25 * 20;
        }

        public void action(int t) {
            if(t % 20 == 0) {
                hooks.addEntity(new SpinnyBall());
            }
            if(rng.nextInt(100) < 10) {
                hooks.addEntity(new Star());
            }
        }    
    }

    private static final int CENTILENGTH = 50;
    private class CentipedeLevel implements Level {        
        private int bodyCount;

        public void initialize(int t) {
            bodyCount = 0;
            hooks.setMessage("Split up the centipede!", 25);
            CentipedePiece prev = null;
            for(int i = 0; i < CENTILENGTH; i++) {
                prev = new CentipedePiece(WIDTH / 2 + 5 * i , - 5 * i, prev);
                ++bodyCount;
                hooks.addEntity(prev);
            }
        }

        public boolean isCompleted(int t) {
            return bodyCount < 1;
        }

        public void action(int t) {
            if(rng.nextInt(100) < 10) {
                hooks.addEntity(new Star());
            }
        }

        private final int CR = 25;
        private class CentipedePiece implements Entity {
            private double cx, cy, tx, ty;
            private CentipedePiece prev;
            private boolean isAlive;
            private void chooseRandomTarget() {
                tx = cx + rng.nextDouble() * 300 - 150;
                ty = cy + rng.nextDouble() * 300 - 150;
                if(tx < 0) { tx = -tx; }
                if(tx > WIDTH) { tx = WIDTH - (tx - WIDTH); }
                if(ty < 0) { ty = -ty; }
                if(ty > HEIGHT) { ty = HEIGHT - (ty - HEIGHT); }
            }

            public CentipedePiece(double cx, double cy, CentipedePiece prev) {
                this.cx = cx; this.cy = cy; this.prev = prev;
                this.isAlive = true;
                if(prev == null) { chooseRandomTarget(); }
            }

            public Shape getShape(int t) {
                return new RoundRectangle2D.Double(cx - CR, cy - CR, 2 * CR, 2 * CR, 5, 5);
            }

            public void render(Graphics2D g2, int t) {
                g2.setPaint(Color.WHITE);
                g2.draw(this.getShape(t));
                g2.setPaint(Color.MAGENTA);
                g2.fill(this.getShape(t));
            }

            public void action(int t) {
                if(prev != null) {
                    if(prev.isActive()) {
                        tx = prev.cx; ty = prev.cy;
                    }
                    else {
                        prev = null;
                        chooseRandomTarget();    
                    }
                }
                cx += (tx - cx) / 10;
                cy += (ty - cy) / 10;
                if(Math.abs(cx - tx) < 10 && Math.abs(cy - ty) < 10) {
                    chooseRandomTarget();
                }
            }

            public boolean isActive() {
                return isAlive;
            }

            public void sendMessage(Entity source, String msg) {
                if(msg.equals("Die!")) {
                    this.isAlive = false;
                    hooks.addEntity(new Explosion(cx, cy));
                    bodyCount--;
                }
                source.sendMessage(this, "Die!");
            }

            public int getZ() { return 2; }   
        }

    }

    private class SpacePlayer extends MouseAdapter implements Entity {
        public double x, y, mouseX;
        private boolean isActive = true;
        public SpacePlayer() {
            x = WIDTH / 2;
            y = HEIGHT - 100;
            hooks.getComponent().addMouseListener(this);
            hooks.getComponent().addMouseMotionListener(this);
        }

        public Shape getShape(int t) {
            return new Rectangle2D.Double(x - 10, y - 10, 20, 20);
        }

        public void render(Graphics2D g2, int t) {
            g2.setColor(Color.RED);
            g2.fill(this.getShape(t));
        }

        public void action(int t) {
            x += (mouseX - x) / 10;
            if(x < 10) { x = 10; }
            if(x > WIDTH - 10) { x = WIDTH - 10; }
            Rectangle2D.Double r = new Rectangle2D.Double(x - 10, y - 10, 20, 20);
            java.util.List<Entity> collisions = hooks.getCollisions(this, null);
            for(Entity e: collisions) {
                e.sendMessage(this, "Crash!");
            }
        }

        public boolean isActive() {
            return isActive;
        }

        public void sendMessage(Entity source, String msg) {
            if(msg.equals("Die!")) {
                this.isActive = false;
                hooks.getComponent().removeMouseListener(this);
                hooks.getComponent().removeMouseMotionListener(this);
                for(int i = 0; i < 5; i++) {
                    hooks.addEntity(new Explosion(x + rng.nextDouble() * 100 - 50,
                            y + rng.nextDouble() * 100 - 50));
                }
            }
        }

        public void mouseMoved(MouseEvent me) {
            mouseX = me.getX();
        }

        public void mousePressed(MouseEvent me) {
            hooks.addEntity(new PewPew(x, y - 11));
        }

        public int getZ() { return 2; }
    }

    private class PewPew extends NewtonEntity {
        public PewPew(double x, double y) {
            this.setX(x);
            this.setY(y);
            this.setVY(-5);
            this.setAY(-0.17);
        }

        public Shape getShape(int t) {
            return new Rectangle2D.Double(getX() - 2, getY() - 10, 4, 10);
        }

        public void render(Graphics2D g2, int t) {
            g2.setPaint(Color.GREEN);
            g2.fill(this.getShape(t));
        }

        public void action(int t) {
            super.action(t);
            java.util.List<Entity> collisions = hooks.getCollisions(this, null);
            if(collisions.size() > 0) {
                for(Entity e: collisions) {
                    hooks.grantPoints(10);
                    e.sendMessage(this, "Die!");
                }
            }
        }

        public boolean isActive() {
            return getY() >= 0;
        }

        public void sendMessage(Entity source, String msg) {
            if(msg.equals("Die!")) { setY(-1000); }
        }

        public int getZ() { return 2; }
    }

    private static final float[] DIST = {0f, 0.3f, .9f, 1f};
    private static final Color[] COLORS = {
        new Color(66, 240, 15), new Color(110, 110, 255), new Color(99, 14, 33), new Color(23, 88, 99)
    };
    private class SpinnyBall extends NewtonEntity {
        private double tx, ty, radius, offset, speed;
        private boolean isAlive = true;

        public SpinnyBall() {
            radius = rng.nextInt(20) + 10;
            offset = rng.nextDouble() * 60;
            speed = rng.nextDouble() * 15 + 5;
            setX(rng.nextDouble() * WIDTH);
            setY(-100);
            setVY(2.0);
            tx = getX();
            ty = getY();
        }

        public Shape getShape(int t) {
            return new Ellipse2D.Double(tx - radius, ty - radius, 2 * radius, 2 * radius);
        }

        public void render(Graphics2D g2, int t) {
            g2.setPaint(new RadialGradientPaint(
            (float)tx, (float)ty, (float)radius,
            (float)(tx + Math.sin(-t / 19.0) * 11.0),
            (float)(ty + Math.cos(-t / 25.0) * 11.0),
            DIST, COLORS, MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g2.fill(this.getShape(t));
        }

        public void action(int t) {
            super.action(t);
            tx = Math.sin((t + offset) / speed) * 50 + getX();
            ty = Math.cos((t + offset) / speed) * 50 + getY();
        }

        public boolean isActive() { 
            return isAlive && getY() < HEIGHT + 50;
        }

        public void sendMessage(Entity source, String msg) {
            if(msg.equals("Die!")) {
                this.isAlive = false;
                hooks.addEntity(new Explosion(tx, ty));
            }
            source.sendMessage(this, "Die!");
        }

        public int getZ() { return 2; }
    }

    private class Star extends NewtonEntity {
        private Color color;
        public Star() {
            this.setX(rng.nextDouble() * WIDTH);
            this.setY(-1);
            int shade = rng.nextInt(255);
            this.color = new Color(shade, shade, shade);
            this.setVY(shade / 100.0);
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

        public boolean isActive() {
            return getY() < HEIGHT;
        }

        public void sendMessage(Entity source, String msg) { }

        public int getZ() { return 0; }
    }

    private static final double[] explosionSize = { 30, 100, 90, 75, 60 };
    private static final Color[] explosionColor = {
            Color.WHITE, new Color(250, 100, 100),
            new Color(150, 50, 50), new Color(100, 20, 20), new Color(50, 0, 0)
        };
    private class Explosion extends NewtonEntity {
        private int age;
        public Explosion(double x, double y) {
            this.setX(x); this.setY(y); this.age = 0;
        }

        public Explosion(double x, double y, double vx, double vy) {
            this(x, y);
            this.setVX(vx); this.setVY(vy);
        }

        public Shape getShape(int t) {
            double r = explosionSize[age] / 2;
            return new Ellipse2D.Double(getX() - r, getY() - r, 2 * r, 2 * r);
        }

        public void render(Graphics2D g2, int t) {
            g2.setPaint(explosionColor[age]);
            g2.fill(this.getShape(t));   
        }

        public void action(int t) { 
            age++;
        }

        public boolean isActive() { return age < explosionSize.length; }

        public void sendMessage(Entity source, String msg) {}

        public int getZ() { return 4; }
    }

    private static final double SIERCUTOFF = 10;
    private static final Color SC1 = new Color(44, 99, 120);
    private static final Color SC2 = new Color(189, 22, 73);
    private class Sierpinski extends NewtonEntity {
        private double width, height;
        public Sierpinski(double x, double y, double width, double height) {
            this.setX(x); this.setY(y); this.width = width; this.height = height;
            this.setVY(2.0);
        }

        public Shape getShape(int t) {
            Polygon p = new Polygon();
            double x = getX(); double y = getY();
            p.addPoint((int)x, (int)y);
            p.addPoint((int)(x + width), (int)y);
            p.addPoint((int)(x + width / 2), (int)(y + height));
            return p;
        }

        public void render(Graphics2D g2, int t) {
            g2.setPaint(new GradientPaint(new Point2D.Double(getX() + width * 0.3, getY()), SC1,
            new Point2D.Double(getX() + width * 0.7, getY() + height), SC2, true));
            g2.fill(this.getShape(t));   
        }

        public void action(int t) {
            super.action(t);
        }

        public boolean isActive() { return width >= SIERCUTOFF && getY() <= HEIGHT; }

        public void sendMessage(Entity source, String msg) {

            if(msg.equals("Die!")) {
                hooks.addEntity(new Explosion(getX() + width / 2, getY() + height / 2));
                width = width / 2;
                height = height / 2;
                if(width >= SIERCUTOFF) {
                    hooks.addEntity(new Sierpinski(getX() + width, getY() + 2, width, height));
                    hooks.addEntity(new Sierpinski(getX() + width / 2, getY() + height + 2, width, height));
                }
            }
            source.sendMessage(this, "Die!");}

        public int getZ() { return 2; }
    }

    public static void main(String[] args) {
        final JFrame f = new JFrame("Space Blasters!");
        final GameEngine blast = new GameEngine(new SpaceGame());
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