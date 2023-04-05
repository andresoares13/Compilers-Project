package pt.up.fe.comp2023.visitors;

import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Map;

public class IdentifierListVisitor extends AJmmVisitor <List<String>,Boolean> {
    @Override
    protected void buildVisitor() {
        addVisit("Identifier", this::visitId);
        setDefaultVisit(this::defaultVisitor);
    }

    private boolean visitId(JmmNode node, List<String> ids){
        ids.add(node.get("value"));
        return true;
    }

    private boolean defaultVisitor(JmmNode node, List<String> ids){
        for (JmmNode child : node.getChildren()) {
            visit(child, ids);
        }
        return true;
    }
}
