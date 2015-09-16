package modules.parts;

import java.awt.Color;
import java.awt.Graphics2D;

import simulator.App;

/**
 * Visible, clickable push switch
 * @author aw12700
 *
 */
public class PushBtn extends TogglePart {

    private LEDColour colour;
    private boolean clicking = false;

	int w = 15;
	int h = 15;

	public PushBtn(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void init() {
	    int type = 0;
	    if (owner.outputs.get(0).link != null)
	        type = owner.outputs.get(0).link.targ.type;

	    switch (type) {
            case Port.CTRL:
                colour = LEDColour.BLUE;
                break;
            default:
                colour = LEDColour.RED;
                break;
	    }
	}

	@Override
	public boolean lbDown(int xPt, int yPt) {
		if (xPt > x-w/2 && xPt < x+w/2 && yPt > y-h/2 && yPt < y+h/2) {
			// Clicked
			setEnabled(true);
			clicking = true;
			App.sim.propagate(owner);
			return true;
		}

		return false;
	}

	@Override
	public boolean lbUp(int xPt, int yPt) {
	    if (clicking) {
	        clicking = false;
	        setEnabled(false);
	        App.sim.propagate(owner);
            return true;
	    }

	    return false;
	}

	@Override
	public void paint(Graphics2D g) {
	    init();
		boolean on = clicking;

		g.setColor(Color.DARK_GRAY);
		g.fillRect(x-w/2, y-h/2, w, h);

		if (on) {
		    g.setColor(colour.light);
			g.fillRect(x-w/2, y-h/2, w, h);
			g.setColor(colour.highlight);
            g.fillRect(x-w/2 + 1, y-h/2+1, w - 2, h - 2);
		}
		else {
		    g.setColor(new Color(150, 150, 150));
			g.fillRect(x-w/2, y-h/2, w, h);
			g.setColor(new Color(130, 130, 130));
            g.fillRect(x-w/2 + 1, y-h/2+1, w - 2, h-3);
		}
	}

}
