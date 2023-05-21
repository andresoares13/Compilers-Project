package pt.up.fe.comp2023.registerAlloc;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;

import java.util.*;

import static pt.up.fe.comp.jmm.report.Stage.OPTIMIZATION;

public class MyLifeTimeCalculator {
    private final Method method;
    private final OllirResult ollirResult;
    private List<Set<String>> in;
    private List<Set<String>> out;
    private List<Set<String>> def;
    private List<Set<String>> use;
    private List<Node> nodes;



    public MyLifeTimeCalculator(Method method, OllirResult ollirResult) {
        this.method = method;
        this.ollirResult = ollirResult;
    }

    private void orderNodes() {
        Node beginNode = method.getBeginNode();
        this.nodes = new ArrayList<>();
        dfsOrderNodes(beginNode, new ArrayList<>());
    }

    private void dfsOrderNodes(Node node, ArrayList<Node> visited) {

        if (node == null
                || nodes.contains(node)
                || visited.contains(node)) {
            return;
        }

        if (node instanceof Instruction instruction && !method.getInstructions().contains(instruction))
            return;

        visited.add(node);

        for (Node successor: node.getSuccessors()) {
            dfsOrderNodes(successor, visited);
        }

        nodes.add(node);
    }

    public void calcInOut() {
        orderNodes();
        in = new ArrayList<>();
        out = new ArrayList<>();
        def = new ArrayList<>();
        use = new ArrayList<>();
        for (Node node: nodes) {
            in.add(new HashSet<>());
            out.add(new HashSet<>());
            def.add(new HashSet<>());
            use.add(new HashSet<>());
            calcUseDef(node);
        }

        boolean characterDevelopment;

        do  {
            characterDevelopment = false;

            for (int index = 0; index < nodes.size(); index++) {
                Node node = nodes.get(index);

                Set<String> origIn = new HashSet<>(in.get(index));
                Set<String> origOut = new HashSet<>(out.get(index));

                out.get(index).clear();

                for (Node successor : node.getSuccessors()) {
                    int successorIndex = nodes.indexOf(successor);
                    if (successorIndex == -1) continue;
                    Set<String> in_successorIndex = in.get(successorIndex);

                    out.get(index).addAll(in_successorIndex);
                }

                in.get(index).clear();

                Set<String> outDefDiff = new HashSet<>(out.get(index));
                outDefDiff.removeAll(def.get(index));

                outDefDiff.addAll(use.get(index));
                in.get(index).addAll(outDefDiff);

                characterDevelopment = characterDevelopment || !origIn.equals(in.get(index)) || !origOut.equals(out.get(index));
            }

        } while (characterDevelopment);
    }

    private void addToUseDefSet(Node node, Element val, List<Set<String>> array) {
        int index = nodes.indexOf(node);

        if (val instanceof ArrayOperand arrayOp) {
            for (Element element: arrayOp.getIndexOperands()) {
                setUse(node, element);
            }
            array.get(index).add(arrayOp.getName());
        }

        if (val instanceof Operand op && !op.getType().getTypeOfElement().equals(ElementType.THIS)) {
            array.get(index).add(op.getName());
        }
    }

    private void setDef(Node node, Element dest) {
        addToUseDefSet(node, dest, def);
    }

    private void setUse(Node node, Element val) {
        addToUseDefSet(node, val, use);
    }

    private void calcUseDef(Node node) {
        calcUseDef(node, null);
    }

    private void calcUseDef(Node node, Node parentNode) {

        if (node == null) return;

        Node useDefNode = parentNode == null ? node : parentNode;

        if (node.getNodeType().equals(NodeType.BEGIN)) {
            return;
        }

        if (node.getNodeType().equals(NodeType.END)) {
            return;
        }

        if (node instanceof AssignInstruction instruction) {
            setDef(useDefNode, instruction.getDest());
            calcUseDef(instruction.getRhs(), node);
        } else if (node instanceof UnaryOpInstruction instruction) {
            setUse(useDefNode, instruction.getOperand());
        } else if (node instanceof BinaryOpInstruction instruction) {
            setUse(useDefNode, instruction.getLeftOperand());
            setUse(useDefNode, instruction.getRightOperand());
        } else if (node instanceof ReturnInstruction instruction) {
            setUse(useDefNode, instruction.getOperand());
        } else if (node instanceof CallInstruction instruction) {
            setUse(useDefNode, instruction.getFirstArg());
            if (instruction.getListOfOperands() != null) {
                for (Element arg: instruction.getListOfOperands()) {
                    setUse(useDefNode, arg);
                }
            }
        } else if (node instanceof GetFieldInstruction instruction) {
            setUse(useDefNode, instruction.getFirstOperand());
        } else if (node instanceof PutFieldInstruction instruction) {
            setUse(useDefNode, instruction.getFirstOperand());
            setUse(useDefNode, instruction.getThirdOperand());
        } else if (node instanceof SingleOpInstruction instruction) {
            setUse(useDefNode, instruction.getSingleOperand());
        } else if (node instanceof OpCondInstruction instruction) {
            for (Element operand: instruction.getOperands()) {
                setUse(useDefNode, operand);
            }
        } else if (node instanceof SingleOpCondInstruction instruction) {
            for (Element operand: instruction.getOperands()) {
                setUse(useDefNode, operand);
            }
        }
    }


    public List<Node> getNodes(){
        return this.nodes;
    }

    public List<Set<String>> getDef(){
        return this.def;
    }

    public List<Set<String>> getOut(){
        return this.out;
    }

    public Method getMethod() {
        return method;
    }
}
