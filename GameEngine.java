import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.*;
import java.util.*;
import java.util.concurrent.*;

public class GameEngine extends JPanel {
        
    private Map<Integer, ArrayList<Entity>> entityMap;
    private TreeSet<Integer> ZLevels;
    
    private java.util.List<Entity> addingEntities; 
    private java.util.List<Level> levels;
    private int entityCount = 0;
    
    private GameHooks hooks;
    private Level currentLevel;
    private java.awt.Dimension dimension;
    private Game game;
    private int nextLevelIdx;
    private int currentTime;
    private int currentScore;
    private int mx, my;
    private String message = null;
    private int messageDelay, messageStart;
    private javax.swing.Timer timer;
    private Semaphore mutex = new Semaphore(1);
    
    public GameEngine(Game game) {
        this.game = game;
        this.setBackground(Color.BLACK);
        this.dimension = game.getDimension();
        this.setPreferredSize(dimension);
        this.setFocusable(true);
        this.requestFocus();
        hooks = new MyGameHooks();
        startNewGame();
        timer = new javax.swing.Timer(40, new GameLoop());        
        timer.start();
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if(ke.getKeyChar() == 'r') {
                    startNewGame();
                }
            } 
        });
    }
    
    private void startNewGame() {
        try {
            mutex.acquire();
            for(MouseListener me: this.getMouseListeners()) {
                this.removeMouseListener(me);
            }
            for(MouseMotionListener me: this.getMouseMotionListeners()) {
                this.removeMouseMotionListener(me);
            }
            entityMap = new java.util.TreeMap<>();
            ZLevels = new TreeSet<Integer>();
            addingEntities = new ArrayList<Entity>();
            levels = game.startNewGame(hooks);
            nextLevelIdx = 0;
            currentTime = 0;
            currentScore = 0;
            message = null;
            startNewLevel();
            this.addMouseMotionListener(new MouseAdapter() {
                public void mouseMoved(MouseEvent me) {
                    GameEngine.this.mx = me.getX();
                    GameEngine.this.my = me.getY();
                }
                public void mouseDragged(MouseEvent me) {
                    GameEngine.this.mx = me.getX();
                    GameEngine.this.my = me.getY();
                }
            });
        }
        catch(InterruptedException e) { }
        finally {
            mutex.release();
        }
    }
    
    private void startNewLevel() {
        if(nextLevelIdx == levels.size()) {
            nextLevelIdx = 0;
        }
        else {
            currentLevel = levels.get(nextLevelIdx++);
            currentLevel.initialize(currentTime);
        }
    }
    
    public void terminate() {
        timer.stop();
        game.terminate();
        System.out.println("Game engine timer terminated");
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g; // convert to better Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON); // looks nicer
        
        try {
            mutex.acquire();
            
            for(Integer i: ZLevels) {
                ArrayList<Entity> activeEntities = entityMap.get(i);
                if(activeEntities != null) {
                    for(Entity e: activeEntities) {
                        if(e.isActive()) { e.render(g2, currentTime); }
                    }
                }
            }
        }
        catch(InterruptedException e) { }
        finally { mutex.release(); }
        g2.setPaint(Color.WHITE);
        g2.drawString(currentScore + "", 20, 20 );
        if(message != null && currentTime - messageStart < messageDelay + 25) {
            if(currentTime - messageStart <= messageDelay) {
                g2.setPaint(Color.WHITE);
            }
            else {
                int shade = 255 - 10 * (currentTime - messageStart - messageDelay);
                g2.setPaint(new Color(shade, shade, shade));
            }
            g2.drawString(message, mx, my - 1);
        }
    }
    
    private class GameLoop implements ActionListener {
        private ArrayList<Integer> entitiesToRemove = new ArrayList<Integer>();
        public void actionPerformed(ActionEvent ae) {
            try {
                mutex.acquire(); 
                currentTime++;
                            
                if(currentLevel != null) {
                    currentLevel.action(currentTime);
                    if(currentLevel.isCompleted(currentTime)) {
                        startNewLevel();
                    }
                }
                for(Integer i: ZLevels) {
                    ArrayList<Entity> activeEntities = entityMap.get(i);
                    for(int idx = 0; idx < activeEntities.size(); idx++) {
                        Entity e = activeEntities.get(idx);
                        if(e.isActive()) {
                            e.action(currentTime);
                        }
                        else {
                            entitiesToRemove.add(idx);
                        }
                    }
                    if(entitiesToRemove.size() > 0) {   
                        for(int j = entitiesToRemove.size() - 1; j >= 0; j--) {
                            int idx = entitiesToRemove.get(j);
                            activeEntities.remove(idx);
                            entityCount--;
                        }
                        entitiesToRemove.clear();
                    }
                }
                for(Entity e: addingEntities) {
                    int z = e.getZ();
                    if(!ZLevels.contains(z)) {
                        entityMap.put(z, new ArrayList<Entity>());
                        ZLevels.add(z);
                    }
                    entityMap.get(z).add(e);
                    entityCount++;
                }
                addingEntities.clear();
            }
            catch(InterruptedException e) { }
            finally {
                mutex.release();
                repaint();
            }
        }
    }
    
    private class MyGameHooks implements GameHooks {
        public void grantPoints(int points) {
            currentScore += points;
        }
        public JComponent getComponent() {
            return GameEngine.this;
        }
        public void addEntity(Entity e) {
            addingEntities.add(e);
        }
        
        public java.util.List<Entity> getCollisions(Entity e, Rectangle2D.Double boundingBox) {
            ArrayList<Entity> collisions = new ArrayList<Entity>();
            Area ea = new Area(e.getShape(currentTime));
            ArrayList<Entity> activeEntities = entityMap.get(e.getZ());
            for(Entity e2: activeEntities) {
                if(e != e2 && e2.isActive()) {
                    Shape s = e2.getShape(currentTime);
                    if(boundingBox != null && !s.intersects(boundingBox)) {
                        continue; // quick rejection
                    }
                    Area a = new Area(s);
                    a.intersect(ea);
                    if(!a.isEmpty()) { collisions.add(e2); }
                 }
            }
            return collisions;
        }
        
        public void setMessage(String message, int delay) {
            GameEngine.this.message = message;
            GameEngine.this.messageStart = currentTime;
            GameEngine.this.messageDelay = delay;
        }
    }
        
    
}