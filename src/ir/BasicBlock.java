package ir;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private final Function parentFunction;
    private final int depth;
    private final ArrayList<Instruction> instructions;

    public BasicBlock(String name, Function parentFunction, int depth) {
        super(name, Type.BasicBlock, false, "void");
        this.parentFunction = parentFunction;
        this.depth = depth;
        this.instructions = new ArrayList<>();
        if(parentFunction != null) {
            parentFunction.addBasicBlock(this);
        }
    }
    public Function getParentFunction() {
        return parentFunction;
    }
    public int getDepth() {
        return depth;
    }
    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }
}

