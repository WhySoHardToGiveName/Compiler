package ir;

public class Constant extends User {
    private int value;

    public Constant(String name, boolean isArr, String dataType, int value) {
        super(name, Type.Const, isArr, dataType);
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
