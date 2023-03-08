package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class ClassesVisitor extends ReportCollector<List<String>, Boolean> {
    @Override
    protected void buildVisitor() {
        addVisit("Program", this::visitProgram);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
    }

    private Boolean visitClassDeclaration(JmmNode classDeclaration, List<String> classes) {
        String className, superName;

        className = classDeclaration.get("name");
        classes.add(className);
        if (classDeclaration.hasAttribute("extend")) {
            superName = classDeclaration.get("extend");
            classes.add(superName);
        }

        return true;
    }

    private Boolean visitProgram(JmmNode program, List<String> classes) {
        for (JmmNode child : program.getChildren()) {
            if (child.getKind().equals("ClassDeclare")) {
                // Process ClassDeclare node here
                System.out.println(child);
                visit(child, classes);
            }
        }
        return true;
    }
}
