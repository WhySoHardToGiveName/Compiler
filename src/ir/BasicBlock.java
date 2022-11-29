package ir;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private final Function parentFunction;
    private final ArrayList<Instruction> instructions;

    public BasicBlock(String name, Function parentFunction) {
        super(name, Type.BasicBlock, false, "void");
        this.parentFunction = parentFunction;
        this.instructions = new ArrayList<>();
        if(parentFunction != null) {
            parentFunction.addBasicBlock(this);
        }
    }
    public static BasicBlock createBBNoAdd2Func(String name, Function parentFunction) {
        BasicBlock bb = new BasicBlock(name, parentFunction);
        parentFunction.popBasicBlock();
        return bb;
    }
    public Function getParentFunction() {
        return parentFunction;
    }
    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName()).append(":\n");
        for (Instruction instruction : instructions) {
            sb.append("  ").append(instruction.toString());
        }
        return sb.toString();
    }
}

