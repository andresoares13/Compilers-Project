package visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class classGenerator extends AJmmVisitor<String, String> {

    public classGenerator() {}

    @Override
    protected void buildVisitor() {
        addVisit("ClassDeclare", this::dealWithClass);
    }

    private String dealWithClass(JmmNode jmmNode, String s) {
        // missing the part inside the class ( varDeclaration )* ( methodDeclaration )*
        return "public class " + visit(jmmNode.getJmmChild(0), "") + "{" + "}";
    }
}
