package tools;

import java.util.ArrayList;
import java.util.List;

import gui.ViewUtil;
import simulator.App;
import simulator.PickableEntity;
import util.ModuleClipboard;
import util.Vec2;
import modules.BaseModule;

public class PlaceTool extends BaseTool {

    private List<PickableEntity> entities = new ArrayList<PickableEntity>();
    private Vec2 start = null;

    /**
     * Creates a placement tool for the specified module
     * @param e
     */
    public PlaceTool(PickableEntity e) {
        App.ui.view.opStack.beginCompoundOp();

        entities.add(e);
        App.sim.addEntity(e); // add module to sim
        App.ui.view.opStack.pushOp(new CreateOperation(e));

        Vec2 p = new Vec2(-200, -200);
        p = ViewUtil.screenToWorld(p);
        e.move(p);
    }

    /**
     * Acts as a 'paste' tool for the given clipboard
     * @param clipboard Clipboard containing modules to 'paste'
     */
    public PlaceTool(ModuleClipboard clipboard) {
        App.ui.view.opStack.beginCompoundOp();
        entities = clipboard.paste();

        for (PickableEntity e : entities) {
            e.tempPos.set(e.pos);
        }
    }

    @Override
    public BaseTool mouseMove(int x, int y) {
        Vec2 cur = ViewUtil.screenToWorld(new Vec2(x, y));

        if (start == null) {
            start = new Vec2(cur);

            Vec2 delta = new Vec2(start);
            delta.sub(entities.get(0).tempPos);

            entities.get(0).tempPos.set(start);
            for (int i = 1; i < entities.size(); i++) {
                entities.get(i).tempPos.add(delta);
            }
        }
        cur.sub(start);

        for (PickableEntity e : entities) {
            e.moveRelative(cur);
        }

        return this;
    }

    @Override
    public BaseTool lbDown(int x, int y) {
        // Make sure the positions are up to date
        mouseMove(x, y);

        if (BaseTool.SHIFT && entities.size() == 1) {
            PickableEntity e = entities.get(0);
            e.enabled = true;

            if (e.getClass().getSuperclass() == BaseModule.class) {
                App.ui.view.opStack.endCompoundOp();
                return new PlaceTool(e.createNew()); // repeat op
            }
        }
        else
        {
            // Select whatever we've just placed
            App.ui.view.clearSelect();
            for (PickableEntity e : entities) {
                e.enabled = true;
                App.ui.view.select(e);
            }
            App.ui.compPane.selected = null;
            App.ui.compPane.repaint();
        }

        // Complete the overall operation
        App.ui.view.opStack.endCompoundOp();
        return null;
    }

    @Override
    public void cancel() {
        App.ui.compPane.selected = null;
        App.ui.compPane.repaint();

        // Cancelling automagically undoes our changes
        App.ui.view.opStack.cancelCompoundOp();

        entities.clear();
    }

}
