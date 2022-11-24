package ir;

import java.util.ArrayList;

public class User extends Value {
    private final ArrayList<Value> operands;

    public User(String name, Type type, boolean isArr, String dataType) {
        super(name, type, isArr, dataType);
        operands = new ArrayList<>();
    }
    public ArrayList<Value> getOperands() {
        return operands;
    }
    public void addOperand(Value operand) {
        operands.add(operand);
    }
}
