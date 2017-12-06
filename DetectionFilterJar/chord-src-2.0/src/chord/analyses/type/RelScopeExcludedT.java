package chord.analyses.type;

import joeq.Class.jq_Class;
import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

@Chord(
		name = "scopeExcludedT",
		sign = "T0"
	)
public class RelScopeExcludedT extends ProgramRel implements IClassVisitor {
	public void visit(jq_Class c) {
		if (Utils.prefixMatch(c.getName(), Config.scopeExcludeAry))
			add(c);
	}

}
