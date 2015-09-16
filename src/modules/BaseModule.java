package modules;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import modules.parts.*;
import simulator.*;
import tools.DeleteOperation;
import util.BinData;
import util.Vec2;

/**
 * Base for module classes
 * @author aw12700
 *
 */
public abstract class BaseModule extends PickableEntity {

    public enum rotationDir {
        ROT_CW,
        ROT_CCW,
        ROT_180
    }

    public double w = 30, h = 30;
    public AffineTransform toWorld = new AffineTransform();
    public AffineTransform toView = new AffineTransform();

    public int orientation = 0;

    public List<Port> ports = new ArrayList<>();
    public List<Input> inputs = new ArrayList<>();
    public List<Output> outputs = new ArrayList<>();
    public List<BidirPort> bidirs = new ArrayList<>();
    public List<VisiblePart> parts = new ArrayList<>();

    public int ID;

    /**
     * Flag used to provide visual error feedback
      */
    public boolean error = false;

    /**
     * Get the object's ID, used for file operations
     * @return The ID
     */
    public int getID() {
        return ID;
    }

    /**
     * Get the module's type as an AvailableModules enumeration
     * @return The module's type
     */
    public abstract AvailableModules getModType();

    /**
     * Adds a new bi-directional port on the input (bottom/right) side
     * @param name User-readable name
     * @param pos Offset from centre
     * @param type Data, control, clock or generic
     * @return The new port
     */
    public BidirPort addBidirInput(String name, int pos, int type) {
        BidirPort p = new BidirPort(1);
        p.text = name;
        p.pos = pos;
        p.type = type;
        p.owner = this;

        bidirs.add(p);
        ports.add(p);

        return p;
    }

    /**
     * Adds a new bi-directional port on the output (top/left) side
     * @param name User-readable name
     * @param pos Offset from centre
     * @param type Data, control, clock or generic
     * @return The new port
     */
    public BidirPort addBidirOutput(String name, int pos, int type) {
        BidirPort p = new BidirPort(-1);
        p.text = name;
        p.pos = pos;
        p.type = type;
        p.owner = this;

        bidirs.add(p);
        ports.add(p);

        return p;
    }

    /**
     * Adds an output
     * @param name User-readable name
     * @param pos Offset from centre
     * @param type Data, control, clock or generic
     * @return The new output
     */
    public Output addOutput(String name, int pos, int type) {
        Output o = new Output();
        o.text = name;
        o.pos = pos;
        o.type = type;
        o.owner = this;

        outputs.add(o);
        ports.add(o);
        return o;
    }

    /**
     * Adds an input
     * @param name User-readable name
     * @param pos Offset from centre
     * @param type Data, control, clock or generic
     * @return The new input
     */
    public Input addInput(String name, int pos, int type) {
        return addInput(name, pos, type, new BinData(0));
    }

    /**
     * Adds an input with a set pull value
     * @param name User-readable name
     * @param pos Offset from centre
     * @param type Data, control, clock or generic
     * @param pullVal 4-bit Binary pull value
     * @return The new input
     */
    public Input addInput(String name, int pos, int type, BinData pullVal) {
        Input i = new Input();
        i.text = name;
        i.pos = pos;
        i.type = type;
        i.owner = this;
        i.pull = pullVal;


        inputs.add(i);
        ports.add(i);
        return i;
    }

    /**
     * Returns ports affected by changes to the given input.
     * Should be overwritten by subclasses to improve loop detector accuracy.
     * @param in Input port to be changed
     */
    public List<Port> getAffected(Port in) {
        List<Port> outList = new ArrayList<>();
        if (in.canInput()) {
            for (Port p : ports) {
                if (p != in && p.canOutput()) {
                    outList.add(p);
                }
            }
        }

        return outList;
    }


    /**
     * Adds a part
     * @param p Part to add
     */
    public void addPart(VisiblePart p) {
        parts.add(p);
        p.owner = this;
    }

    /**
     * Displays the module in local space
     * @param g Graphics context to render with
     */
    public abstract void paint(Graphics2D g);

    /**
     * Displays the module's bounding box
     * @param g Graphics context to render with
     */
    @Override
    public void drawBounds(Graphics2D g) {
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(2));
        g.drawRect((int)(- w/2) - 2, (int)(- h/2) - 2, (int)w + 2, (int)h + 2);
    }

    /**
     * Draws the visible parts
     * @param g Graphics context to render with
     */
    protected void drawParts(Graphics2D g) {
        for (VisiblePart p : parts) {
            p.paint(g);
        }
    }

    /**
     * Draws the outputs as arrows
     * @param g Graphics context to render with
     */
    protected void drawOutputs(Graphics2D g) {
        g.setStroke(new BasicStroke(2));

        for (Output o : outputs) {
            boolean side = (o.type == Port.CTRL || o.type == Port.CLK);

            int aw = 10;
            int offset = o.pos;

            if (side) offset = - offset;

            int[] aPoints = {-aw + offset, -aw + offset, aw + offset, aw + offset, offset};

            // Base offset
            int base, angle;
            if (!side) {
                base = -(int)h/2;
                angle = 0;
            }
            else {
                base = -(int)w/2;
                angle = 90;
            }

            int[] bPoints = {base+aw, base, base, base+aw, base};

            // Draw internal shape
            if (!side)
                g.fillPolygon(aPoints, bPoints, 5);
            else
                g.fillPolygon(bPoints, aPoints, 5);

            Color oldC = g.getColor();

            if (o.type == Port.GENERIC)
                g.setColor(Color.GRAY);
            else if (o.type == Port.CTRL)
                g.setColor(Color.BLUE);
            else if (o.type == Port.CLK)
                g.setColor(new Color(100, 160, 100));
            else if (o.type == Port.DATA)
                g.setColor(Color.RED);

            if (!side)
                g.fillArc(offset-5, base - 5, 10, 10, angle, 180);
            else
                g.fillArc(base - 5, offset - 5, 10, 10, angle, 180);

            g.setColor(oldC);
        }
    }

    /**
     * Draws the inputs as arrows
     * @param g Graphics context to render with
     */
    protected void drawInputs(Graphics2D g) {
        g.setStroke(new BasicStroke(2));

        // Loop the inputs
        for (Input i : inputs) {
            boolean side = (i.type == Port.CTRL || i.type == Port.CLK);

            int aw = 10;
            int offset = i.pos;

            if (side) offset = - offset;

            // Base offset
            int base, angle;
            if (!side) {
                base = (int)h/2;
                angle = 180;
            }
            else {
                base = (int)w/2;
                angle = 270;
            }

            int[] aPoints;
            int[] bPoints;
            int num;
            //if (i.bidir) {
            //    aPoints = new int[] {-aw + offset, -aw + offset, aw + offset, aw + offset, offset};
            //    bPoints = new int[] {base-aw, base, base, base-aw, base};
            //    num = 5;
            //}
            //else {
                aPoints = new int[]{-aw + offset, aw + offset, offset};
                bPoints = new int[]{base, base, base-aw};
                num = 3;
            //}

            // Draw internal shape
            if (!side)
                g.fillPolygon(aPoints, bPoints, num);
            else
                g.fillPolygon(bPoints, aPoints, num);

            Color oldC = g.getColor();

            if (i.type == Port.GENERIC)
                g.setColor(Color.GRAY);
            else if (i.type == Port.CTRL)
                g.setColor(Color.BLUE);
            else if (i.type == Port.CLK)
                g.setColor(new Color(100, 160, 100));
            else if (i.type == Port.DATA)
                g.setColor(Color.RED);

            int x = offset-5;
            int y = base - 5;
            if (side) {
                int temp = x;
                x = y;
                y = temp;
            }

            //if (i.bidir)
            //    g.fillArc(x, y, 10, 10, angle, 180);
            //else
                g.drawArc(x, y, 10, 10, angle, 180);

            g.setColor(oldC);
        }
    }

    /**
     * Draws the bidirectional ports as arrows
     * @param g Graphics context to render with
     */
    protected void drawBidir(Graphics2D g) {
        g.setStroke(new BasicStroke(2));

        for (BidirPort bp : bidirs) {
            int aw = -10;
            int offset = bp.pos;

            // Base offset
            int base, angle;
            base = (int) h / 2;
            angle = (bp.side == 1) ? 180 : 0;
            base *= bp.side;

            // Points for output-style wedge
            int[] aPoints_wedge = {-aw + offset, -aw + offset, aw + offset, aw + offset, offset};
            int[] bPoints_wedge = {base + bp.side*aw, base, base, base + bp.side*aw, base};

            // Points for input-style arrow
            int[] aPoints_arrow = new int[]{offset - aw, offset + aw, offset};
            int[] bPoints_arrow = new int[]{base, base, base + bp.side*aw};

            // Draw internal shape
            if (bp.getMode() == Port.Mode.MODE_OUTPUT) {
                g.fillPolygon(aPoints_wedge, bPoints_wedge, 5);
            }
            else if (bp.getMode() == Port.Mode.MODE_INPUT) {
                g.fillPolygon(aPoints_arrow, bPoints_arrow, 3);
            }

            Color oldC = g.getColor();

            if (bp.type == Port.GENERIC)
                g.setColor(Color.GRAY);
            else if (bp.type == Port.CTRL)
                g.setColor(Color.BLUE);
            else if (bp.type == Port.CLK)
                g.setColor(new Color(100, 160, 100));
            else if (bp.type == Port.DATA)
                g.setColor(Color.RED);

            // Drawing style depends on port input/output mode
            if (bp.getMode() == Port.Mode.MODE_BIDIR || bp.getMode() == Port.Mode.MODE_OUTPUT) {
                g.fillArc(offset - 5, base - 5, 10, 10, angle, 180);
            } else {
                g.drawArc(offset - 5, base - 5, 10, 10, angle, 180);
            }

            g.setColor(oldC);
        }
    }

    /**
     * Draws the module as a trapezoid
     * @param g Graphics context to render with
     * @param corner Corner size in pixels
     */
    protected void drawTrapezoid(Graphics2D g, int corner) {
        drawTrapezoid(g, corner, 0, 0, (int) w, (int) h);
    }

    /**
     * Draws a trapezoid with the specified dimensions
     */
    protected void drawTrapezoid(Graphics2D g, int corner, int x, int y, int w, int h) {
        int[] xPoints = {x-w/2, x+w/2, x+w/2,        x+w/2-corner, x-w/2+corner, x-w/2};
        int[] yPoints = {y+h/2, y+h/2, y-h/2+corner, y-h/2,        y-h/2,        y-h/2 + corner};
        g.fillPolygon(xPoints, yPoints, 6);
    }

    /**
     * Draws the module as a box
     * @param g Graphics context to render with
     * @param corner Corner size in pixels
     */
    protected void drawBox(Graphics2D g, int corner) {
        int iw = (int)w, ih = (int)h;
        int[] xPoints = {-iw/2, -iw/2+corner, iw/2-corner, iw/2, iw/2, iw/2-corner, -iw/2+corner, -iw/2};
        int[] yPoints = { ih/2-corner, ih/2, ih/2, ih/2-corner, -ih/2+corner, -ih/2, -ih/2, -ih/2+corner};
        g.fillPolygon(xPoints, yPoints, 8);
    }

    /**
     * Rotates the module
     * @param dir Which direction to rotate in
     */
    public final void rotate(rotationDir dir) {
        switch (dir) {
            case ROT_CW:
                orientation = (orientation + 1) % 4;
                break;
            case ROT_CCW:
                orientation = (orientation - 1) % 4;
                break;
            case ROT_180:
                orientation = (orientation + 2) % 4;
                break;
        }
    }

    /**
     * Updates the object's transformation
     */
    public void updateXForm() {
        snapToGrid();

        toWorld = new AffineTransform();
        toWorld.translate(pos.x, pos.y);
        toWorld.rotate((Math.PI / 2) * orientation);

        toView = new AffineTransform(App.ui.view.wToV);
        toView.concatenate(toWorld);

        // Update links
        for (Output o : outputs) {
            if (o.link != null) o.link.updatePath();
        }
        for (Input i : inputs) {
            if (i.link != null) i.link.updatePath();
        }
    }

    /**
     * Generates on-grid coords
     */
    public void snapToGrid() {
        pos.x = Math.round(pos.x / App.sim.grid) * App.sim.grid;
        pos.y = Math.round(pos.y / App.sim.grid) * App.sim.grid;
    }

    /**
     * Transforms a point from object to world-space
     * @param p Point
     * @return Transformed point
     */
    public Vec2 objToWorld(Vec2 p) {
        double[] pt = p.asArray();

        toWorld.transform(pt, 0, pt, 0, 1);

        return new Vec2(pt);
    }

    /**
     * Removes a module from the sim. DOES NOT affect its links.
     * Creates a new delete operation.
     */
    @Override
    public void delete() {
        App.sim.removeEntity(this);
        App.ui.view.deselect(this);

        App.ui.view.opStack.pushOp(new DeleteOperation(this));
    }

    /**
     * Handles user interaction through parts
     * @param ix X coord in view space
     * @param iy Y coord in view space
     * @return Whether the input was handled
     */
    public boolean lbDown(int ix, int iy) {
        if (!enabled) return false;

        // Coords in object space
        double[] pt = {ix, iy};
        try {toView.inverseTransform(pt, 0, pt, 0, 1);}
        catch (Exception e) {e.printStackTrace();}

        int dx = (int)pt[0];
        int dy = (int)pt[1];

        synchronized (parts) {
            for (VisiblePart p : parts) {
                if (p.lbDown(dx, dy)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handles user interaction through parts
     * @param ix X coord in view space
     * @param iy Y coord in view space
     * @return Whether the input was handled
     */
    public boolean lbUp(int ix, int iy) {
        if (!enabled) return false;

        // Coords in object space
        double[] pt = {ix, iy};
        try {toView.inverseTransform(pt, 0, pt, 0, 1);}
        catch (Exception e) {e.printStackTrace();}

        int dx = (int)pt[0];
        int dy = (int)pt[1];

        synchronized (parts) {
            for (VisiblePart p : parts) {
                if (p.lbUp(dx, dy)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean within(double x, double y, double x2, double y2) {
        double[] rect = {x, y, x2, y2};

        // Get clicked point in object space
        try {toWorld.inverseTransform(rect, 0, rect, 0, 2);}
        catch (Exception e) {
            System.err.println("Non inversible transform");
        }

        x  = Math.min(rect[0], rect[2]);
        y  = Math.min(rect[1], rect[3]);
        x2 = Math.max(rect[0], rect[2]);
        y2 = Math.max(rect[1], rect[3]);

        if (    x < - w/2 && x2 > w/2 &&
                y < - h/2 && y2 > h/2 )
            return true;
        else return false;
    }

    @Override
    public boolean intersects(Vec2 pt) {
        double[] dpt = pt.asArray();

        // Get clicked point in object space
        try {toWorld.inverseTransform(dpt, 0, dpt, 0, 1);}
        catch (Exception e) {
            System.err.println("Non invertible transform");
        }

        double nx = dpt[0];
        double ny = dpt[1];

        if (    nx > - w/2 && nx < w/2 &&
                ny > - h/2 && ny < h/2 ) {
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public int getType() {
        return PickableEntity.MODULE;
    }

    /**
     * Updates the module's outputs based on its inputs
     * (Needs override)
     */
    public abstract void propagate();

    /**
     * Propagates a bidirectional port's directionality.<br/>Note: this is recursive through the setMode() calls!
     * @param root Port to base directionality on
     */
    public void propagateDirectionality(BidirPort root) {
        for (BidirPort p : bidirs) {
            if (p == root) continue;
            Port.Mode rootMode = root.getMode();

            if (p.side == root.side) {
                p.setMode(rootMode);

                if (p.link != null) {
                    if (rootMode == Port.Mode.MODE_BIDIR) {
                        p.link.targ.setMode(rootMode);
                        p.link.src.setMode(rootMode);
                    }
                    else if (p.link.src == p) {
                        p.link.targ.setMode(Port.OppositeOf(rootMode));
                    }
                    else if (p.link.targ == p) {
                        p.link.src.setMode(Port.OppositeOf(rootMode));
                    }
                }
            }
            else if (p.side == -root.side) {
                p.setMode(Port.OppositeOf(rootMode));

                if (p.link != null) {
                    if (rootMode == Port.Mode.MODE_BIDIR) {
                        p.link.targ.setMode(rootMode);
                        p.link.src.setMode(rootMode);
                    }
                    else if (p.link.src == p) {
                        p.link.targ.setMode(rootMode);
                    }
                    else if (p.link.targ == p) {
                        p.link.src.setMode(rootMode);
                    }
                }
            }
        }
    }

    /**
     * Run tests on the module
     * @return True if tests ran successfully
     */
    public boolean test() {return true;}

    /**
     * Initialize state with a loaded hash map structure (module-specific implementation)
     * Called by XMLReader and copy routines. Default behaviour is no-op.
     * @param data Structure containing state to load (module-defined elements)
     */
    public void dataIn(HashMap<String, String> data) {}

    /**
     * Fill a string-string hash map with module-specific data for retrieval with dataIn.
     * Called by XMLWriter and the copy routines. Default behaviour is to return null, indicating that no relevant
     * @return A filled hash map structure, or null if no state is stored
     */
    public HashMap<String, String> dataOut() { return null; }

    public enum AvailableModules {
        // Enum members should not be renamed!
        ADDSUB(new AddSub(), "Arithmetic Unit"),
        CLOCK(new Clock(), "Clock"),
        DEMUX(new Demux(), "Demultiplexor"),
        FANOUT(new Fanout(), "Fanout"),
        LOGIC(new Logic(), "Logic Unit"),
        MUX(new Mux(), "Multiplexor"),
        OR(new Or(), "OR"),
        RAM(new NRAM(true), "NRAM"),
        REGISTER(new Register(), "Register"),
        LEFT_SHIFT(new Shift(true), "Left-shift"),
        RIGHT_SHIFT(new Shift(false), "Right-shift"),
        SPLIT_MERGE(new SplitMerge(), "Splitter / Merger"),
        SWITCH(new SwitchInput(), "Switch Input");

        /**
         * The module represented by this enum value, to use to instantiate and display in GUI.
         */
        private final BaseModule module;
        private final String name;

        AvailableModules(BaseModule mod, String name) {
            this.module = mod;
            this.name = name;


        }

        public BaseModule getSrcModule() {
            return module;
        }

        @Override
        public String toString() {
            return name;
        }

        public static AvailableModules fromModule(BaseModule mod) throws IllegalArgumentException {
            for (AvailableModules am : values()) {
                if (am.module.getClass().equals(mod.getClass())) {
                    return am;
                }
            }

            throw new IllegalArgumentException("Module of type " + mod.getClass() + " is not available!");
        }
    }

}
