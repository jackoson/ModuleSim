package modules;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import modules.parts.Port;
import simulator.Main;
import tools.DeleteOperation;
import util.BezierPath;
import util.BinData;

/**
 * Link between two ports
 * @author aw12700
 *
 */
public class Link {

    public Port src;
    public Port targ;
    public BezierPath curve;
    public int linkInd;

    /**
     * Creates a new link between two ports, which may be reversed depending on type of source and target.
     * @param source The first clicked port
     * @param target The second clicked port
     * @param path A bezier path to display for the link
     * @return New link, or null if link was invalid
     */
    public static Link createLink(Port source, Port target, BezierPath path) {

    	if (source == null || target == null) {
    	    System.err.println("No connect: Port(s) do not exist");
    	    return null;
    	}

    	if (source == target) {
    	    JOptionPane.showMessageDialog(null, "Cannot link port to itself");
    	    System.err.println("No connect: Ports are the same");
    	    return null;
    	}

        if (source.owner == target.owner) {
            JOptionPane.showMessageDialog(null, "Cannot link module to itself");
            System.err.println("No connect: Same module");
            return null;
        }

        // If two directional ports are either both outputs or both inputs, they cannot be linked
        if (source.canOutput() == target.canOutput() && source.hasDirection() && target.hasDirection()) {
            JOptionPane.showMessageDialog(null, "Cannot link same port types together");
            System.err.println("No connect: Same port types");
            return null;
        }
        else {
            // Start a compound operation (likely nested) so we can abort cleanly
            Main.ui.view.opStack.beginCompoundOp();

            // Cleanup old links
            if (source.link != null) {
                source.link.delete();
            }
            if (target.link != null) {
                target.link.delete();
            }

            Link newLink = new Link();
            source.link = newLink;
            target.link = newLink;

            // Pick direction of link

            // If we're between two bi-dirs, the user's word is law
            if (!source.hasDirection() && !target.hasDirection()) {
                // (attempt to) create link in direction chosen
                newLink.src = source;
                newLink.targ = target;
                newLink.curve = path;

                source.setMode(Port.Mode.MODE_OUTPUT);
                target.setMode(Port.Mode.MODE_INPUT);
            }
            else if (source.canOutput() && target.canInput()) {
                newLink.src = source;
                newLink.targ = target;
                newLink.curve = path;

                if (!source.hasDirection()) {
                    source.setMode(Port.Mode.MODE_OUTPUT);
                }

                if (!target.hasDirection()) {
                    target.setMode(Port.Mode.MODE_INPUT);
                }
            }
            else if (source.canInput() && target.canOutput()) {
                newLink.src = target;
                newLink.targ = source;
                path.reverse();
                newLink.curve = path;

                if (!source.hasDirection()) {
                    source.setMode(Port.Mode.MODE_INPUT);
                }

                if (!target.hasDirection()) {
                    target.setMode(Port.Mode.MODE_OUTPUT);
                }
            }
            else {
                JOptionPane.showMessageDialog(null, "Unknown error during link creation");
                Main.ui.view.opStack.cancelCompoundOp();
                return null;
            }

            // Check loops
            List<BaseModule> modules = new ArrayList<>();
            if (newLink.checkLoops(newLink, modules)) {
                JOptionPane.showMessageDialog(null, "Link would create a loop. Have you forgotten a register?");
                System.err.println("No connect: Loop detected");
                source.link = null;
                target.link = null;

                for (BaseModule m : modules) {
                    m.error = true;
                }

                Main.ui.view.opStack.cancelCompoundOp();
                return null;
            }

            // Changes are done
            Main.ui.view.opStack.endCompoundOp();

            // Propagate
            newLink.targ.setVal(newLink.src.getVal());
            Main.sim.propagate(newLink.targ.owner);

            return newLink;
        }
    }

    /**
     * Recursively check for loops in the design
     * @param check Link to check for
     * @param modules List of modules checked - used for error display
     * @return Whether the check link was found
     */
    private boolean checkLoops(Link check, List<BaseModule> modules) {
        // Registers & NRAM *should* terminate loops
        BaseModule.AvailableModules type = targ.owner.getModType();
        if (type == BaseModule.AvailableModules.RAM || type == BaseModule.AvailableModules.REGISTER) return false;

        // Follow every affected outbound link
        boolean result = false;
        for (Port p : targ.owner.getAffected(targ)) {
            if (p.canOutput() && p.link != null) {
                if (p.link == check) {
                    modules.add(targ.owner);
                    return true; // Loop detected
                }
                else {
                    result |= p.link.checkLoops(check, modules);
                    if (result) modules.add(targ.owner);
                }
            }
        }

        return result;
    }

    /**
     * Draw the link - colour is picked based on the port types
     * @param g Graphics context to draw with
     */
    public void draw(Graphics2D g) {
        if (src.type == Port.CLK || targ.type == Port.CLK){
            g.setColor(new Color(100, 160, 100));
        }
        else if (src.type == Port.GENERIC && targ.type == Port.GENERIC) {
            g.setColor(Color.GRAY);
        }
        else if (	(src.type == Port.DATA || src.type == Port.GENERIC) && targ.type == Port.DATA ||
                src.type == Port.DATA && targ.type == Port.GENERIC) {
            g.setColor(Color.RED);
        }
        else if (	(src.type == Port.CTRL  || src.type == Port.GENERIC) && targ.type == Port.CTRL ||
                src.type == Port.CTRL && targ.type == Port.GENERIC) {
            g.setColor(Color.BLUE);
        }
        else {
            // Mixed
            g.setColor(new Color(255, 0, 100));
        }

        g.setStroke(new BasicStroke(2));
        curve.draw(g);
    }

    /**
     * Updates the Bezier curve for display
     */
    public void updatePath() {
        // Generate the curve
        curve.setStart(src);
        curve.setEnd(targ);
        curve.calcCurves();
    }

    /**
     * Removes the link, along with references to it at the remaining ports.
     * The change causes a simulation propagation from the target module.
     * This method creates a new deletion operation.
     */
    public void delete() {
        src.link = null;
        targ.link = null;

        // Propagate change
        targ.setVal(new BinData());
        Main.sim.propagate(targ.owner);

        // Propagate (non-)directionality if applicable
        src.setMode(Port.Mode.MODE_BIDIR);
        targ.setMode(Port.Mode.MODE_BIDIR);

        // Remove from listings
        Main.sim.removeLink(this);

        // Store operation
        Main.ui.view.opStack.pushOp(new DeleteOperation(this));
    }

}