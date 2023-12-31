package pt.up.fe.comp2023.visitors;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public abstract class ReportCollector<D, R> extends AJmmVisitor<D, R> {
    final protected List<Report> reports = new ArrayList<>();

    public List<Report> getReports() {
        return reports;
    }

    public void addSemanticErrorReport(JmmNode node, String message) {
        int line = Integer.parseInt(node.get("line"));
        int column = Integer.parseInt(node.get("col"));
        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, column, message));

    }
}