package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.visitors.ImportsVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTableStore implements SymbolTable {

    private List<String> imports = new ArrayList<>();
    private String className;
    private List<Symbol> fields;


    public SymbolTableStore(JmmParserResult parserResult) {
        ImportsVisitor importsVisitor = new ImportsVisitor();

        importsVisitor.visit(parserResult.getRootNode(),this.imports);

    }

    @Override
    public List<String> getImports() {

        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String s) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return null;
    }

    public void printImports(){
        System.out.println(imports.size());
        for (int i = 0; i<this.imports.size();i++){
            System.out.println(this.imports.get(i));
        }
    }
}
