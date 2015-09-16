package modules.parts;

import java.awt.Color;
import java.awt.Graphics2D;

import simulator.App;

/**
 * Visible, clickable toggle switch
 * @author aw12700
 *
 */
public class Switch extends TogglePart {

    private LEDColour colour;

	int w = 15;
	int h = 25;

    public Switch(int x, int y) {
        this.x = x;
        this.y = y;
    }

	public Switch(int x, int y, LEDColour col) {
		this.x = x;
		this.y = y;
		colour = col;
	}

    public void setColour(LEDColour col) {
        colour = col;
    }

	@Override
	public boolean lbDown(int xPt, int yPt) {
		if (xPt > x-w/2 && xPt < x+w/2 && yPt > y-h/2 && yPt < y+h/2) {
			// Clicked
			toggleEnabled();
			App.sim.propagate(owner);
			return true;
		}

		return false;
	}

	@Override
	public void paint(Graphics2D g) {
		boolean on = getEnabled();

		g.setColor(Color.DARK_GRAY);
		g.fillRect(x-w/2, y-h/2, w, h);

		if (on) {
		    g.setColor(colour.light);
			g.fillRect(x-w/2, y-11, w, 15);
			g.setColor(colour.highlight);
            g.fillRect(x-w/2 + 1, y-10, w - 2, 12);
		}
		else {
		    g.setColor(new Color(150, 150, 150));
			g.fillRect(x-w/2, y-3, w, 15);
			g.setColor(new Color(130, 130, 130));
            g.fillRect(x-w/2 + 1, y-1, w - 2, 12);
		}
	}

}
