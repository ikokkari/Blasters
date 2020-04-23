import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.awt.event.*;

public class TTFE implements Game {

    private static final int SIZE = 4, WIDTH = 600, HEIGHT = 600, BORDER = 30;
    private static final int MOVESPEED = 5;
    private static final int STARFREQ = 5;
    private static int TILE;
    private GameHooks hooks;
    private Random rng = new Random();
    private TTFELevel gameLevel = new TTFELevel();

    private Color getColor(int v) {
        int r = (201 * v) % 235 + 20;
        int g = (143 * v) % 235 + 20;
        int b = (79 * v) % 235 + 20;
        return new Color(r, g, b, 200);
    }

    public String getTitle() { return "2048"; }
    public String getAuthor() { return "Ilkka Kokkarinen"; }
    
    public java.util.List<Level> startNewGame(GameHooks hooks) {
        this.hooks = hooks;
        TILE = (WIDTH - 2 * BORDER) / SIZE;
        Level[] levels = { gameLevel };
        return Arrays.asList(levels);
    }

    public Dimension getDimension() {
        return new Dimension(WIDTH, HEIGHT);
    }

    public void terminate() {
        gameLevel.terminate();
    }

    private class TTFELevel implements Level {

        private TTFETile[][] tiles;
        private int nextMovingCheck = 0;
        private boolean moving = false;
        private int dx, dy;
        private MyKeyListener keyListener = null;
        private int highest = 3;

        private boolean addRandomTile(int v) {
            int x, y;
            boolean spaceExists = false;
            for(x = 0; x < SIZE; x++) {
                for(y = 0; y < SIZE; y++) {
                    if(tiles[x][y] == null) { spaceExists = true; }
                }
            }
            if(!spaceExists) { return false; }
            do {
                x = rng.nextInt(SIZE);
                y = rng.nextInt(SIZE);
            } while(tiles[x][y] != null);
            TTFETile tile = new TTFETile(x, y, v);
            tiles[x][y] = tile;
            hooks.addEntity(tile);
            return true;
        }

        public void initialize(int t) {
            tiles = new TTFETile[SIZE][SIZE];
            addRandomTile(1 + rng.nextInt(2));
            addRandomTile(1 + rng.nextInt(2)); 
            if(keyListener == null) {
                keyListener = new MyKeyListener();
                hooks.getComponent().addKeyListener(keyListener);
            }
        }

        public void terminate() {
            hooks.getComponent().removeKeyListener(keyListener);
        }

        public boolean isCompleted(int t) { 
            return false;
        }

        public void action(int t) { 
            if(t % STARFREQ == 0) { hooks.addEntity(new Star()); }
            if(!moving || t < nextMovingCheck) { return; }
            boolean somebodyMoves = false;
            for(int x = 0; x < SIZE; x++) {
                for(int y = 0; y < SIZE; y++) {
                    int nx = x + dx; int ny = y + dy;
                    if(tiles[x][y] != null && nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE) {
                        if(tiles[nx][ny] == null || (tiles[nx][ny].getValue() == tiles[x][y].getValue())) {
                            somebodyMoves = true;
                            tiles[x][y].setTimeToCheck(t + MOVESPEED);
                            double gx = BORDER + nx * TILE;
                            double gy = BORDER + ny * TILE;
                            tiles[x][y].setLinearPath(gx, gy, t, t + MOVESPEED + 1);
                            tiles[x][y].setStopAtEnd(true);
                            tiles[x][y] = null;
                        }
                    }
                }
            }
            if(somebodyMoves) { nextMovingCheck = t + MOVESPEED + 1; }
            else {
                moving = false;
                if(nextMovingCheck > 0) { addRandomTile(1 + rng.nextInt(2)); }
            }
        }

        private class MyKeyListener extends KeyAdapter {
            public void keyPressed(KeyEvent ke) {
                if(moving) { return; }
                int kc = ke.getKeyCode();
                if(kc == KeyEvent.VK_LEFT) { dx = -1; dy = 0; }
                else if(kc == KeyEvent.VK_RIGHT) { dx = +1; dy = 0; }
                else if(kc == KeyEvent.VK_UP) { dx = 0; dy = -1; }
                else if(kc == KeyEvent.VK_DOWN) { dx = 0; dy = +1; }
                else { return; }
                moving = true;
                nextMovingCheck = 0;
            }
        }

        private class TTFETile extends BezierEntity {
            private int v;
            private boolean isAlive = true;
            private int timeToCheck;
            public void setTimeToCheck(int t) { timeToCheck = t; }

            public int getValue() { return v; }

            public TTFETile(int tx, int ty, int v) {
                this.v = v;
                this.setX(BORDER + tx * TILE);
                this.setY(BORDER + ty * TILE);
            }

            public Shape getShape(int t) {
                int v2 = 3*(v+1);
                Area a = new Area(new Rectangle2D.Double(getX(), getY(), TILE, TILE));
                a.subtract(new Area(new Ellipse2D.Double(getX() + TILE/2 - v2, getY() + TILE/2 - v2, 2*v2, 2*v2)));
                return a;
            }

            public void render(Graphics2D g2, int t) {
                g2.setPaint(getColor(v));
                g2.fill(this.getShape(t));
            }

            public void action(int t) {
                super.action(t);
                if(timeToCheck == t) {
                    int tx = (int)Math.round((this.getX() - BORDER) / TILE);
                    int ty = (int)Math.round((this.getY() - BORDER) / TILE);
                    if(tiles[tx][ty] == null) { tiles[tx][ty] = this; }
                    else if(tiles[tx][ty] != this) {
                        hooks.grantPoints(1 << (tiles[tx][ty].v)++);
                        if(highest < tiles[tx][ty].v) {
                            highest = tiles[tx][ty].v;
                            hooks.setMessage("Created tile " + (1 << tiles[tx][ty].v) + "!", 25);
                        }
                        this.isAlive = false;
                    }
                }
            }

            public boolean isActive() { return this.isAlive; }
            public int getZ() { return 1; }
        }

        private class Star extends NewtonEntity {
            private int age;
            public Star() {
                age = 0;
                this.setX(WIDTH / 2);
                this.setY(HEIGHT / 2);
                double d = rng.nextDouble() * 2 * Math.PI;
                this.setVY(Math.sin(d) * 3);
                this.setVX(Math.cos(d) * 3);
            }

            public Shape getShape(int t) {
                return new Rectangle2D.Double(getX(), getY(), 3, 3);
            }

            public void render(Graphics2D g2, int t) {
                int shade = 3 * age;
                if(shade > 254) { shade = 254; }
                g2.setPaint(new Color(shade, shade, shade));
                g2.fill(this.getShape(t));
            }

            public void action(int t) {
                super.action(t);
                age++;
                setAX(getVX() * 0.01);
                setAY(getVY() * 0.01);
                
            }

            public boolean isActive() {
                return getX() >= 0 && getY() >= 0 && getX() <= WIDTH && getY() <= HEIGHT;
            }

            public int getZ() { return 0; }
        }
    }

    public static void main(String[] args) {
        final JFrame f = new JFrame("2048!");
        final GameEngine ttfe = new GameEngine(new TTFE());
        f.add(ttfe);
        // Must ensure that timer is stopped when the Frame closes
        f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    ttfe.terminate();
                    f.dispose();
                }
            });
        f.pack();
        f.setVisible(true);        
    }
}
