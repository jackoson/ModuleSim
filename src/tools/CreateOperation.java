package tools;

import modules.Link;
import modules.parts.Port;
import simulator.App;
import simulator.PickableEntity;
import util.BinData;

/**
 * Created by Ali on 31/08/2015.
 */
public class CreateOperation extends BaseOperation {

    public CreateOperation(PickableEntity e) { entity = e; }

    public CreateOperation(Link l) {
        link = l;
    }

    @Override
    public void undo() {
        if (entity != null) App.sim.removeEntity(entity);
        if (link != null) {
            link.src.link = null;
            link.targ.link = null;
            App.sim.removeLink(link);

            // Propagate change
            link.targ.setVal(new BinData());
            App.sim.propagate(link.targ.owner);

            link.src.setMode(Port.Mode.MODE_BIDIR);
            link.targ.setMode(Port.Mode.MODE_BIDIR);
        }
    }

    @Override
    public void redo() {
        if (entity != null) App.sim.addEntity(entity);
        if (link != null) {
            link.src.link = link;
            link.targ.link = link;
            App.sim.addLink(link);

            // Propagate change
            link.src.setMode(Port.Mode.MODE_OUTPUT);
            link.targ.setMode(Port.Mode.MODE_INPUT);
            link.targ.setVal(link.src.getVal());
            App.sim.propagate(link.targ.owner);
        }
    }
}
