package ir;

public class Arg extends Value {
    private final Function parentFunction;
    private final int argNo;

    public Arg(String name, Type type, boolean isArr, Function parentFunction, int argNo, String dataType) {
        super(name, type, isArr, dataType);
        this.parentFunction = parentFunction;
        this.argNo = argNo;
        parentFunction.addArg(this);
    }
}
