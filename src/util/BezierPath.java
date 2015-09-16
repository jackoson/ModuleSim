package util;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import modules.parts.Port;
import simulator.App;

/**
 * Manages a path consisting of connected bezier curves
 * @author aw12700
 *
 */
public class BezierPath {

	public List<BezierCurve> curves = new ArrayList<BezierCurve>();
	protected ArrayList<CtrlPt> ctrlPts = new ArrayList<CtrlPt>();

	/**
	 * Defines an initial curve
	 * @param start The start-point
	 * @param c1off Offset for the first control point (from start)
	 * @param end The end-point
	 * @param c2off Offset for the second control point (from end)
	 */
	public BezierPath(Vec2 start, Vec2 c1off, Vec2 end, Vec2 c2off) {
		Vec2 c1 = new Vec2(start);
		c1.add(c1off);

		Vec2 c2 = new Vec2(end);
		c2.add(c2off);

		curves.add(new BezierCurve(start, end, c1, c2));
	}

	/**
	 * Creates an "empty" path
	 */
	public BezierPath() {
		curves.add(new BezierCurve(new Vec2(), new Vec2(), new Vec2(), new Vec2()));
	}

	/**
	 * Creates a duplicate path
	 * @param curve
	 */
	public BezierPath(BezierPath curve) {
	    this();

        for (CtrlPt c : curve.ctrlPts) {
            addPt((CtrlPt) c.createNew());
        }
        calcCurves();
    }

    /**
	 * Moves the startpoint
	 */
	public void setStart(Vec2 end) {
		BezierCurve first = curves.get(0);
		first.p1 = new Vec2(end);
		first.c1 = new Vec2(end);
		first.update();
	}
	public void setStart(Vec2 end, Vec2 c) {
		BezierCurve first = curves.get(0);
		first.p1 = new Vec2(end);
		first.c1 = new Vec2(c);
		first.update();
	}
	public void setStart(Port p) {
		Vec2 c1;

		if (p.type == Port.CTRL || p.type == Port.CLK)
			c1 = new Vec2(p.side * App.sim.grid * 2, 0);
		else
			c1 = new Vec2(0, p.side * App.sim.grid * 2);

		c1.add(p.getDisplayPos());
		c1 = p.owner.objToWorld(c1);

		setStart(p.getDisplayPosW(), c1);
	}

	/**
	 * Moves the endpoint
	 */
	public void setEnd(Vec2 end) {
		setEnd(end, end);
	}
	public void setEnd(Vec2 end, Vec2 c) {
		BezierCurve last = curves.get(curves.size() - 1);
		last.p2 = new Vec2(end);
		last.c2 = new Vec2(c);

		updateContours();
	}
	public void setEnd(Port p) {
		Vec2 c1;

		if (p.type == Port.CTRL || p.type == Port.CLK)
			c1 = new Vec2(p.side* App.sim.grid*2, 0);
		else
			c1 = new Vec2(0, p.side* App.sim.grid*2);

		c1.add(p.getDisplayPos());
		c1 = p.owner.objToWorld(c1);

		setEnd(p.getDisplayPosW(), c1);
	}

	/**
	 * Updates the curve list
	 */
	public void calcCurves() {
		Vec2 start = curves.get(0).p1;
		Vec2 startC = curves.get(0).c1;

		Vec2 end = curves.get(curves.size() - 1).p2;
		Vec2 endC = curves.get(curves.size() -1).c2;

		curves.clear();

		// First curve - start pt and first ctrl pt
		curves.add(new BezierCurve());
		setStart(start, startC);

		// Iterate the control points
		for (int i = 0; i < ctrlPts.size(); i++) {
			ctrlPts.get(i).index = i;
			Vec2 pt = ctrlPts.get(i).pos;

			// Update existing curve
			curves.get(i).p2 = pt;

			// Add next curve
			BezierCurve c = new BezierCurve();
			c.p1 = pt;
			curves.add(c);
		}

		// Set end
		setEnd(end, endC);

		// Update the contours
		updateContours();
	}

	/**
	 * Updates the (intermediate) curves to new control points
	 */
	public void updateContours() {
		for (int i = 0; i < curves.size(); i++) {
			BezierCurve last2 = null;
			if (i - 1 >= 0) {
				last2 = curves.get(i - 1);
			}
			BezierCurve last = curves.get(i);

			Vec2 compPt;
			if (last2 != null) {
				compPt = last2.p1;
			}
			else {
				compPt = last.p1;
			}

			// Delta
			Vec2 d = new Vec2(last.p2);
			d.sub(compPt);

			if (last2 != null) {
				// last.c1 offset = d
				Vec2 c = new Vec2(d);
				c.setLen(last.len() / 2);
				c.add(last.p1);
				last.c1 = c;

				// last2.c2 offset = -d
				c = new Vec2(d);
				c.setLen(last2.len() / 2);
				c.neg();
				c.add(last2.p2);
				last2.c2 = c;
			}

			last.update();
			if (last2 != null) last2.update();
		}
	}

	/**
	 * Adds a control point
	 */
	public void addPt(CtrlPt pt) {
	    pt.parent = this;
		pt.index = ctrlPts.size();
		ctrlPts.add(pt);
		calcCurves();
	}

	/**
	 * Adds a control point at the specified position
	 * @param index Index to add point at. Must be < ctrlPts.size()
	 * @param pt The control point to add
	 */
	public void addPt(int index, CtrlPt pt) {
		pt.parent = this;
		if (index <= ctrlPts.size()) {
			pt.index = index;
			ctrlPts.add(index, pt);
			calcCurves();
		}
		else {
			throw new IndexOutOfBoundsException("Attempted to add control point at out-of-bounds index");
		}
	}

	/**
	 * Removes the last control point
	 * @return True if a point was removed
	 */
	public boolean removePt() {
		if (ctrlPts.size() > 0) {
			CtrlPt pt = ctrlPts.remove(ctrlPts.size() - 1);
			App.sim.removeEntity(pt);
			calcCurves();
			return true;
		}

		return false;
	}

	/**
	 * Remove the specified point from the path
	 * @param ctrlPt Point to remove
	 */
    public void removePt(CtrlPt ctrlPt) {
        ctrlPts.remove(ctrlPt);
        App.sim.removeEntity(ctrlPt);
        calcCurves();
    }

    /**
     * Get a copy of the list of control points
     * @return A clone (copy) of the internal control points list
     */
    @SuppressWarnings("unchecked")
    public List<CtrlPt> getCtrlPts() {
        return (List) ctrlPts.clone();
    }

	/**
	 * Reverses the path
	 */
	public void reverse() {
		ArrayList<CtrlPt> newPts = new ArrayList<CtrlPt>();

		for (int i = ctrlPts.size() - 1; i >= 0; i--) {
			newPts.add(ctrlPts.get(i));
		}

		ctrlPts = newPts;
		calcCurves();
	}

	/**
	 * Draw the full path
	 * @param g Graphics context to draw with
	 */
	public void draw(Graphics2D g) {
		// Draw lines
		for (BezierCurve c : curves) {
			c.draw(g);
		}

		// Draw control points
		for (CtrlPt c : ctrlPts) {
		    c.draw(g);
		    if (c.selected) c.drawBounds(g);
		}
	}

}
