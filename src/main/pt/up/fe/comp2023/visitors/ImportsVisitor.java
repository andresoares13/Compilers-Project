package pt.up.fe.comp2023.visitors;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ImportsVisitor extends ReportCollector<List<String>, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit("ImportDeclaration", this::visitImportDeclaration);
    }


    private Boolean visitImportDeclaration(JmmNode importDeclaration, List<String> imports) {
        String temp;

        temp = importDeclaration.get("name").replaceAll("\\[|\\]", ""); // remove the brackets
        String[] strArr = temp.split(", ");
        List<String> strList = Arrays.asList(strArr);

        temp = "";
        for (int i=0;i<strList.size();i++){
            temp += strList.get(i) + ".";
        }
        temp = temp.substring(0, temp.length() - 1);
        imports.add(temp);

        return true;
    }
}
