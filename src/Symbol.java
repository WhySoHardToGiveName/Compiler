import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Symbol {
    private final String name;
    private final String type;
    private final String reg;
    private final int declareRow;
    private final ArrayList<Integer> useRowList = new ArrayList<>();
    private final boolean isConst;
    private int constValue;
    private final boolean isArray;
    private final int arrayDim;
    private final boolean isFunc;
    public class FuncInfo{
        public int paramNum;
        public ArrayList<String> paramTypeList;
        public ArrayList<Integer> paramArrayDimList;
        public boolean isVoid;
    }
    private FuncInfo funcInfo;

    public Symbol(String name, String type, String reg, int declareRow, boolean isConst, boolean isArray, int arrayDim, boolean isFunc){
        this.name = name;
        this.type = type;
        this.reg = reg;
        this.declareRow = declareRow;
        this.isConst = isConst;
        this.isArray = isArray;
        this.arrayDim = arrayDim;
        this.isFunc = isFunc;
    }
    public String getName(){
        return name;
    }
    public String getType(){
        return type;
    }
    public int getDeclareRow(){
        return declareRow;
    }
    public ArrayList<Integer> getUseRowList(){
        return useRowList;
    }
    public void addUseRow(int useRow){
        useRowList.add(useRow);
    }
    public boolean isConst(){
        return isConst;
    }
    public boolean isArray(){
        return isArray;
    }
    public int getArrayDim(){
        return arrayDim;
    }
    public boolean isFunc(){
        return isFunc;
    }
    public void setFuncInfo(int paramNum, ArrayList<String> paramTypeList, ArrayList<Integer> paramArrayDimList, boolean isVoid){
        funcInfo = new FuncInfo();
        funcInfo.paramNum = paramNum;
        funcInfo.paramTypeList = paramTypeList;
        funcInfo.paramArrayDimList = paramArrayDimList;
        funcInfo.isVoid = isVoid;
    }
    public FuncInfo getFuncInfo(){
        return funcInfo;
    }
    public String getReg(){
        return reg;
    }
    public void setConstValue(int constValue){
        this.constValue = constValue;
    }
    public int getConstValue(){
        return constValue;
    }
}
