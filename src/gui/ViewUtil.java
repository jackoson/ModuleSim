package gui;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import modules.*;
import modules.parts.BidirPort;
import modules.parts.Input;
import modules.parts.Output;
import modules.parts.Port;
import simulator.*;
import tools.*;
import util.Vec2;

/**
 * Listens for mouse events on the view.
 * Handles most user interaction with the view via the tools system.
 * @author aw12700
 *
 */
public class ViewUtil implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private double cStartX, cStartY;
    private double oldCX, oldCY;
    private boolean camDrag = false;

    /**
     * Tests collision with ports
     */
    public static Port portAt(double x, double y) {
        double portR = 10;

        synchronized (App.sim) {
            int i = App.sim.getModules().size() - 1;
            for (; i >= 0; i--) {
                BaseModule m = App.sim.getModules().get(i);

                double[] pt = {x, y};

                // Get clicked point in object space
                try {m.toView.inverseTransform(pt, 0, pt, 0, 1);}
                catch (Exception e) {
                    System.err.println("Non invertible transform");
                }

                Vec2 v = new Vec2(pt);

                // Test outputs
                for (Output o : m.outputs) {
                    Vec2 p = new Vec2();

                    if (o.type == Port.CTRL || o.type == Port.CLK) {
                        p.x = -m.w / 2;
                        p.y = -o.pos;
                    }
                    else {
                        p.x = o.pos;
                        p.y = -m.h / 2;
                    }

                    if (v.dist(p) < portR) {
                        return o;
                    }
                }

                // Test Inputs
                for (Input in : m.inputs) {
                    Vec2 p = new Vec2();

                    if (in.type == Port.CTRL || in.type == Port.CLK) {
                        p.x = m.w / 2;
                        p.y = -in.pos;
                    }
                    else {
                        p.x = in.pos;
                        p.y = m.h / 2;
                    }

                    if (v.dist(p) < portR) {
                        return in;
                    }
                }

                // Test Bidir
                for (BidirPort bd : m.bidirs) {
                    Vec2 p = new Vec2();

                    if (bd.type == Port.CTRL || bd.type == Port.CLK) {
                        p.x = bd.side * m.w / 2;
                        p.y = -bd.pos;
                    }
                    else {
                        p.x = bd.pos;
                        p.y = bd.side * m.h / 2;
                    }

                    if (v.dist(p) < portR) {
                        return bd;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Tests collision with entities
     * @param x
     * @param y
     * @return
     */
    public static PickableEntity entityAt(double x, double y) {
        synchronized (App.sim) {
            double[] pt = {x, y};

            try {
                App.ui.view.wToV.inverseTransform(pt, 0, pt, 0, 1);}
            catch (Exception e) {
                System.err.println("Non inversible transform");
            }

            Vec2 clickPt = new Vec2(pt);

            // Loop the entities
            for (PickableEntity e : App.sim.getEntities()) {
                if (e.intersects(clickPt)) {
                    return e;
                }
            }
        }

        return null;
    }

    /**
     * Find entities within the specified area
     * @param x X coord of rect in screen space
     * @param y Y coord of rect in screen space
     * @param w width of rect
     * @param h height of rect
     * @return
     */
    public static List<PickableEntity> entitiesWithin(double x, double y, double x2, double y2) {
        List<PickableEntity> result = new ArrayList<PickableEntity>();

        synchronized (App.sim) {
            double[] pt = {x, y, x2, y2};

            try {
                App.ui.view.wToV.inverseTransform(pt, 0, pt, 0, 2);}
            catch (Exception e) {
                System.err.println("Non inversible transform");
            }

            x = pt[0];
            y = pt[1];
            x2 = pt[2];
            y2 = pt[3];

            // Loop the entities
            for (PickableEntity e : App.sim.getEntities()) {
                if (e.within(x, y, x2, y2)) {
                    result.add(e);
                }
            }
        }

        return result;
    }

    /**
     * Adjusts an on screen point to world-space
     */
    public static Vec2 screenToWorld(Vec2 p) {
        double[] pt = p.asArray();

        try {
            App.ui.view.wToV.inverseTransform(pt, 0, pt, 0, 1);}
        catch (Exception e) {}

        return new Vec2(pt);
    }

    /**
     * Adjusts a world point to screen-space
     */
    public static Vec2 worldToScreen(Vec2 p) {
        double[] pt = p.asArray();

        App.ui.view.wToV.transform(pt, 0, pt, 0, 1);

        return new Vec2(pt);
    }

    public void mouseClicked(MouseEvent e) {
        testKeys(e);

        if (e.getButton() == MouseEvent.BUTTON3) {
            // Right-click
            PickableEntity targ = entityAt(e.getX(), e.getY());

            if (targ != null) {
                App.ui.view.select(targ);
                App.ui.popup.showEntityMenu(App.ui.view.selection, e.getX(), e.getY());
            }
            else {
                App.ui.popup.showEntityMenu(App.ui.view.selection, e.getX(), e.getY());
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
        App.ui.view.requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        testKeys(e);

        if (e.getButton() == MouseEvent.BUTTON1) {
            // Left click handled by tools
            BaseTool tool = App.ui.view.curTool;

            PickableEntity targ = entityAt(e.getX(), e.getY());

            // See if module handles interaction - otherwise, use tools
            boolean handled = false;
            if (targ != null && targ.getType() == PickableEntity.MODULE) {
                BaseModule m = (BaseModule) targ;
                handled = m.lbDown(e.getX(), e.getY());
            }

            if (!handled) {
                if (tool != null) {
                    App.ui.view.curTool = tool.lbDown(e.getX(), e.getY());
                }
                else {
                    Port p = portAt(e.getX(), e.getY());

                    //Link behaviour
                    if (p != null) {
                        tool = new LinkTool();
                        App.ui.view.curTool = tool.lbDown(e.getX(), e.getY());
                    }
                    // Selection behaviour
                    else {
                        tool = new SelectTool();
                        App.ui.view.curTool = tool.lbDown(e.getX(), e.getY());
                    }
                }
            }
        }
        else if (e.getButton() == MouseEvent.BUTTON3) {
            // Camera behaviour
            cStartX = e.getX();
            cStartY = e.getY();
            oldCX = App.ui.view.camX;
            oldCY = App.ui.view.camY;
            camDrag = true;
        }
    }

    public void mouseReleased(MouseEvent e) {
        testKeys(e);
        BaseTool tool = App.ui.view.curTool;

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (tool != null) {
                App.ui.view.curTool = tool.lbUp(e.getX(), e.getY());
            }
            else {
                PickableEntity targ = entityAt(e.getX(), e.getY());
                if (targ == null) {
                    App.ui.view.clearSelect();
                }
                else if (targ.getType() == PickableEntity.MODULE) {
                    BaseModule m = (BaseModule) targ;
                    m.lbUp(e.getX(), e.getY());
                }
            }
        }
        else if (e.getButton() == MouseEvent.BUTTON3) {
            camDrag = false;
        }
    }

    public void mouseDragged(MouseEvent e) {
        testKeys(e);
        BaseTool tool = App.ui.view.curTool;

        if (camDrag) {
            App.ui.view.camX = oldCX + e.getX() - cStartX;
            App.ui.view.camY = oldCY + e.getY() - cStartY;
        }
        else if (tool != null) {
            App.ui.view.curTool = tool.mouseDrag(e.getX(), e.getY());
        }
    }

    public void mouseMoved(MouseEvent e) {
        testKeys(e);
        BaseTool tool = App.ui.view.curTool;

        Port p = portAt(e.getX(), e.getY());
        if (p != null) {
            if (p.hasDirection()) {
                App.ui.view.setToolTipText(p.text + " - " + p.getVal().toString() + (p.canOutput() ? " OUT" : " IN"));
            }
            else {
                App.ui.view.setToolTipText(p.text + " - Disconnected");
            }
        }
        else {
            App.ui.view.setToolTipText(null);
        }

        if (tool != null) {
            App.ui.view.curTool = tool.mouseMove(e.getX(), e.getY());
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        testKeys(e);
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            if (e.getWheelRotation() < 0) {
                App.ui.view.zoomIn(e.getX(), e.getY());
            }
            else {
                App.ui.view.zoomOut(e.getX(), e.getY());
            }
        }
    }

    public void keyPressed(KeyEvent e) {
        View v = App.ui.view;

        // Cancel tool usage on escape press
        if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
            if (v.curTool != null) {
                v.curTool.cancel();
                v.curTool = null;
            }
        }
        // Delete selection
        else if (e.getKeyChar() == KeyEvent.VK_DELETE) {
            v.deleteSelection();
        }
        // Pass to tool
        else if (v.curTool != null) {
            v.curTool = v.curTool.keyDown(e.getKeyCode());
        }
    }

    public void keyReleased(KeyEvent e) {
        View v = App.ui.view;
        if (v.curTool != null) {
            v.curTool.keyUp(e.getKeyCode());
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void testKeys(MouseEvent e) {
        BaseTool.CTRL = e.isControlDown();
        BaseTool.SHIFT = e.isShiftDown();
    }

}
