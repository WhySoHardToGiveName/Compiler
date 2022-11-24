package ir;

public class Use {
    private User user;
    private Value value;
    private int operandNo; // the operandNo of the user

    public Use(User user, Value value, int operandNo) {
        this.user = user;
        this.value = value;
        this.operandNo = operandNo;
        value.addUse(this);
        user.addOperand(value);
    }
    public User getUser() {
        return user;
    }
    public Value getValue() {
        return value;
    }
    public int getOperandNo() {
        return operandNo;
    }
}
