package visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class importsGenerator extends AJmmVisitor<String, String> {

    public importsGenerator() {}

    @Override
    protected void buildVisitor() {
        addVisit("ImportDeclare", this::dealWithImports);
    }

    private String dealWithImports(JmmNode jmmNode, String s) {
        String impt = "import ";
        StringBuilder ret = new StringBuilder(s + impt);
        List<JmmNode> nodes = jmmNode.getChildren();
        int i = 0;
        for(JmmNode node: nodes) {
            if(i != 0) ret.append(".");
            ret.append(visit(node.getJmmChild(i), ""));
            i++;
        }

        return ret.toString();
    }
}
