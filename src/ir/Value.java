package ir;

import java.util.ArrayList;
import java.util.LinkedList;

public class Value {
    private final String name;
    private final Type type;
    private String dataType;
    private final LinkedList<Use> useList;
    private boolean isArray;
    private ArrayInfo arrayInfo;
    private boolean inverseCond = false;
    public class ArrayInfo{
        public int arrayDim;
        public int arraySize1;
        public int arraySize2;
    }

    public Value(String name, Type type, boolean isArray, String dataType) {
        this.name = name;
        this.type = type;
        this.isArray = isArray;
        this.dataType = dataType;
        this.useList = new LinkedList<>();
    }
    public void setArrayInfo(int arrayDim, ArrayList<Value> arraySizeList){
        this.arrayInfo = new ArrayInfo();
        this.arrayInfo.arrayDim = arrayDim;
        this.arrayInfo.arraySize1 = ((Constant)arraySizeList.get(0)).getValue();
        this.arrayInfo.arraySize2 = arrayDim == 2 ? ((Constant)arraySizeList.get(1)).getValue() : 0;
    }
    public boolean isArray() {
        return isArray;
    }
    public void setDataType(String dataType){
        this.dataType = dataType;
    }
    public ArrayInfo getArrayInfo(){
        return this.arrayInfo;
    }
    public String getName() {
        return name;
    }
    public Type getType() {
        return type;
    }
    public LinkedList<Use> getUseList() {
        return useList;
    }
    public void addUse(Use use) {
        useList.add(use);
    }
    public String getDataType() {
        return dataType;
    }
    public boolean isConstant() {
        return type == Type.Const;
    }
    public boolean isGlobalConstant() {
        if(this instanceof GlobalVariable) {
            return ((GlobalVariable) this).isGlobalConstant();
        }
        return false;
    }
    public boolean isInverseCond() {
        return inverseCond;
    }
    public void inverseCond() {
        inverseCond = !inverseCond;
    }
}
