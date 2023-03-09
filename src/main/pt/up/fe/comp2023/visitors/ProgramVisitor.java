package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgramVisitor extends ReportCollector<Map<String,Object>, Boolean>  {

    @Override
    protected void buildVisitor() {
        this.addVisit("Program",this::programVisit);
    }
    boolean programVisit(JmmNode node, Map<String,Object> programInfo){
        ImportsVisitor importsVisitor = new ImportsVisitor();
        ClassesVisitor classesVisitor= new ClassesVisitor();
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ClassDeclare")) {
                Map<String,Object> classInfo = new HashMap<>(programInfo);
                classInfo.remove("imports"); //classInfo only keeps relevant info for class.
                classesVisitor.visit(child,classInfo);
            } else if (child.getKind().equals("ImportDeclare")){
                importsVisitor.visit(child, (List<String>) programInfo.get("imports"));
            }
        }
        return true;
    }
}
