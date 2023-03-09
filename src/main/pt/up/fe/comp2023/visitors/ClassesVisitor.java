package pt.up.fe.comp2023.visitors;

import org.antlr.v4.runtime.misc.Triple;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SymbolTableStore;

import java.util.List;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;

public class ClassesVisitor extends ReportCollector<Map<String,Object>, Boolean> {
    @Override
    protected void buildVisitor() {
        addVisit("ClassDeclaration", this::visitClassDeclaration);
    }

    private Boolean visitClassDeclaration(JmmNode node, Map<String, Object> classInfo) {
        ((SymbolTableStore.StringReference)classInfo.get("className")).string = node.get("name");
        if (node.hasAttribute("extend")) {
            ((SymbolTableStore.StringReference)classInfo.get("superName")).string = node.get("extend");
        }
        //visit children
        MethodsVisitor methodsVisitor = new MethodsVisitor();
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("VarDeclare")) {
                visitField(child, (List<Symbol>) classInfo.get("fields"));
            } else if (child.getKind().startsWith("MethodDeclare")) {//could be main too, thus startswith
                methodsVisitor.visit(child,
                        (Map<String, Triple<Type, List<Symbol>, List<Symbol>>>) classInfo.get("methods_parameters")
                );
            }
        }
        return true;
    }

    boolean visitField(JmmNode node,List<Symbol> fields){
        JmmNode typeNode=node.getJmmChild(0);
        Symbol symbol = new Symbol(
                new Type(
                        typeNode.get("name"),
                        parseBoolean(typeNode.get("isArray"))
                )
                ,
                node.get("name")
        );
        fields.add(symbol);
        return true;
    }
}
