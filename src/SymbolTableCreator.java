import ir.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

public class SymbolTableCreator {
    private final error_handler errorHandler;
    private SymbolTable curTable;
    private int cycleDepth = 0;
    private int regNo = 0;
    private Function curFunction;
    private final Module module;
    private boolean isGlobal = true;
    private BasicBlock curbb;
    private final Stack<BasicBlock> whileCondBBStack = new Stack<>();
    private final Stack<BasicBlock> whileNextBBStack = new Stack<>();
    public SymbolTableCreator(error_handler errorHandler){
        this.errorHandler = errorHandler;
        module = Module.getModule();
        curbb = module;
    }

    private Function getFunction(String nameWithNoAt){
        for (Function function : module.getFuncList()) {
            if (function.getName().equals("@" + nameWithNoAt)) {
                return function;
            }
        }
        return null;
    }
    private Value getValue(String name, BasicBlock curbb){
        if (name.charAt(0) == '@') {
            for (GlobalVariable globalVariable : module.getGlobalVariables()) {
                if (globalVariable.getName().equals(name)) {
                    return globalVariable;
                }
            }
            return null;
        } else if (name.charAt(0) == '%') {
            ArrayList<Instruction> instructions = curbb.getParentFunction().getInstructions();
            for (Instruction instruction : instructions) {
                if (instruction.getName().equals(name)) {
                    return instruction;
                }
            }
        }
        return null;
    }
    //  获得元素的Value并load
    private Value getArrItemValue(Value arr, ArrayList<Value> indexList){
        if(arr instanceof GlobalVariable) {
            if(((GlobalVariable) arr).getOperands().size() == 0)    // 初始化为0
                return new Constant("0", false, "int", 0);
            if(arr.isGlobalConstant() && indexList.size() - 1 == arr.getArrayInfo().arrayDim) {  // 直接返回常量
                boolean allConst = true;
                for (Value index : indexList) {
                    if (!index.isConstant()) {
                        allConst = false;
                        break;
                    }
                }
                if(allConst){
                    int offset = 0;
                    if(arr.getArrayInfo().arrayDim == 1)
                        offset = ((Constant)indexList.get(1)).getValue();
                    else if(arr.getArrayInfo().arrayDim == 2)
                        offset = ((Constant)indexList.get(1)).getValue() * arr.getArrayInfo().arraySize2 + ((Constant)indexList.get(2)).getValue();
                    return ((GlobalVariable) arr).getOperands().get(offset);
                }
            }
        }
        Instruction gep = GEP(arr, indexList);
        return loadInstr(gep);
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public SymbolTable CompUnit(GrammarTree compUnitNode){
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.setParentTable(null);
        this.curTable = symbolTable;
        ArrayList<GrammarTree> kidTreeList = compUnitNode.getKidTreeList();
        for (GrammarTree kidTree : kidTreeList) {
            switch (kidTree.getVname()) {
                case "Decl":
                    isGlobal = true;
                    Decl(kidTree);
                    break;
                case "FuncDef":
                    isGlobal = false;
                    FuncDef(kidTree);
                    break;
                case "MainFuncDef":
                    isGlobal = false;
                    MainFuncDef(kidTree);
                    break;
            }
        }
        System.out.println("error analysis done!");
        System.out.println("symbol table created");
        System.out.println("LLVM IR generated");
        return symbolTable;
    }

    // Decl → ConstDecl | VarDecl
    // done
    public void Decl(GrammarTree declNode){
        ArrayList<GrammarTree> kidTreeList = declNode.getKidTreeList();
        if(kidTreeList.size() > 1) {
            System.err.println("Error: Decl node has more than one kid.");
            return;
        }
        GrammarTree kidTree = kidTreeList.get(0);
        switch (kidTree.getVname()){
            case "ConstDecl":
                ConstDecl(kidTree);
                break;
            case "VarDecl":
                VarDecl(kidTree);
                break;
        }
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' // i✔
    // done
    public void ConstDecl(GrammarTree constDeclNode){
        ArrayList<GrammarTree> kidTreeList = constDeclNode.getKidTreeList();
        GrammarTree bTypeNode = kidTreeList.get(1);
        String bType = BType(bTypeNode);
        for(int i = 2; i < kidTreeList.size() - 1; i++){
            GrammarTree constDefNode = kidTreeList.get(i);
            if(constDefNode.getVname().equals("ConstDef")){
                ConstDef(constDefNode, bType);
            }
        }
    }

    // BType → 'int'
    public String BType(GrammarTree bTypeNode){
        return bTypeNode.getKidTreeList().get(0).getVname();
    }

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal  // b k✔
    // done
    public void ConstDef(GrammarTree constDefNode, String bType){
        ArrayList<GrammarTree> kidTreeList = constDefNode.getKidTreeList();
        VtNode identNode = (VtNode) kidTreeList.get(0);
        if(errorHandler.hasErrorB(curTable, identNode)){        //处理错误b
            errorHandler.handleErrorB(identNode);
            return;
        }
        boolean isArray = false;
        int arrayDim = 0, i = 1;
        ArrayList<Value> arraySizeList = new ArrayList<>();
        while(i < kidTreeList.size() && kidTreeList.get(i).getVname().equals("[")){
            isArray = true;
            arrayDim++;
            Value arraySize = ConstExp(kidTreeList.get(i + 1));
            arraySizeList.add(arraySize);
            i += 3;
        }
        ArrayList<Value> arrValueList = isArray ? new ArrayList<>() : null;
        Value initValue = ConstInitVal(kidTreeList.get(kidTreeList.size() - 1), arrValueList);      //数组初始化得到arrValueList，非数组初始化得到initValue
        Token token = identNode.getToken();
        User defValue;
        if(isGlobal){       // @a = global i32 5        @c = dso_local constant [2 x [1 x i32]] [[1 x i32] [i32 1], [1 x i32] [i32 3]]
            defValue = new GlobalVariable("@" + token.getTokenValue(), Type.Var, isArray, "int", true, module);
            if(isArray) {
                for (int j = 0; j < arrValueList.size(); j++)
                    new Use(defValue, arrValueList.get(j), j + 1);
                defValue.setArrayInfo(arrayDim, arraySizeList);
                defValue.setDataType(getStringDataType(arraySizeList));
            }
            else
                new Use(defValue, initValue, 1);
        } else {             // %1=alloca i32   %2=store i32 5, i32* %1    //   alloca GEP memset GEP store
            if(isArray){
                defValue = allocaInstr(true, arraySizeList, "int");    // %1 = alloca [2 x [2 x i32]]  并设置arrayInfo
                localArrInit(arrayDim, arraySizeList, arrValueList, defValue);
            } else {
                defValue = allocaInstr(false, null, "int");
                storeInstr(initValue, defValue);
            }
        }
        Symbol constSymbol = new Symbol(token.getTokenValue(), bType, defValue.getName(), token.getRow(), true, isArray, arrayDim, false);
        curTable.addSymbol(constSymbol);
    }

    private void localArrInit(int arrayDim, ArrayList<Value> arraySizeList, ArrayList<Value> arrValueList, User arrValue) {
        ArrayList<Value> indexList = new ArrayList<>();
        for(int j = 1; j <= arrayDim + 1; j++)
            indexList.add(new Constant("0", false, "int", 0));
        Instruction gep = GEP(arrValue, indexList);         // %2 = getelementptr [2 x [2 x i32]], [2 x [2 x i32]]* %1, i32 0, i32 0, i32 0
        int size = 1;       //数组元素个数
        for(int j = 0; j < arrayDim; j++)
            size *= ((Constant) arraySizeList.get(j)).getValue();
        Instruction memsetInstr = new Instruction("@memset", Type.Void, false, curbb, Instruction.OPType.call, "void"); // call void @memset(i32* %2, i32 0, i32 16)
        new Use(memsetInstr, Objects.requireNonNull(getFunction("memset")), 1);
        new Use(memsetInstr, gep, 2);
        new Use(memsetInstr, new Constant("0", false, "int", 0), 3);
        new Use(memsetInstr, new Constant(String.valueOf(4*size), false, "int", 4*size), 4);
        for (int j = 0; j < size; j++){
            ArrayList<Value> itemIndexList = new ArrayList<>();
            itemIndexList.add(new Constant(String.valueOf(j), false, "int", j));
            Instruction itemGEP = GEP(gep, itemIndexList);     // %3 = getelementptr i32, i32* %2, i32 0
            storeInstr(arrValueList.get(j), itemGEP);          // store i32 1, i32* %3
        }
    }

    // ConstInitVal → ConstExp
    //    | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    // 如果是数组，将初始化的值放在数组里,{{1,2},{3,4}} -> [1,2,3,4]
    public Value ConstInitVal(GrammarTree constInitValNode, ArrayList<Value> arrayValueList){
        ArrayList<GrammarTree> kidTreeList = constInitValNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            if(arrayValueList != null){
                arrayValueList.add(ConstExp(kidTreeList.get(0)));
                return null;
            }
            return ConstExp(kidTreeList.get(0));
        }else{
            for (GrammarTree kidTree : kidTreeList) {
                if(kidTree.getVname().equals("ConstInitVal")){
                    ConstInitVal(kidTree, arrayValueList);
                }
            }
        }
        return null;
    }

    // VarDecl → BType VarDef { ',' VarDef } ';' // i✔
    // done
    public void VarDecl(GrammarTree varDeclNode){
        ArrayList<GrammarTree> kidTreeList = varDeclNode.getKidTreeList();
        GrammarTree bTypeNode = kidTreeList.get(0);
        String bType = BType(bTypeNode);
        for(int i = 1; i < kidTreeList.size() - 1; i++){
            GrammarTree varDefNode = kidTreeList.get(i);
            if(varDefNode.getVname().equals("VarDef")){
                VarDef(varDefNode, bType);
            }
        }
    }

    private Instruction allocaInstr(boolean isArr, ArrayList<Value> arraySizeList, String dataType){
        String valueName = "%" + regNo;
        regNo++;
        if(!isArr)
            return new Instruction(valueName, Type.Var, false, curbb, Instruction.OPType.alloca, dataType);
        String stringDataType = getStringDataType(arraySizeList);
        Instruction arr =  new Instruction(valueName, Type.Var, true, curbb, Instruction.OPType.alloca, stringDataType);
        arr.setArrayInfo(arraySizeList.size(), arraySizeList);
        return arr;
    }

    private static String getStringDataType(ArrayList<Value> arraySizeList) {
        String dataType = "[" + arraySizeList.get(0).getName() + " x ";
        if(arraySizeList.size() == 1)
            dataType += "i32]*";
        else
            dataType += "[" + arraySizeList.get(1).getName() + " x i32]]*";
        return dataType;
    }

    private Instruction GEP(Value value, ArrayList<Value> indexList){
        String valueName = "%" + regNo;
        regNo++;
        boolean isArr = value.isArray() && indexList.size() - 1 < value.getArrayInfo().arrayDim;
        Instruction GEPInstr = new Instruction(valueName, Type.Var, isArr, curbb, Instruction.OPType.getelementptr, "i32*");
        new Use(GEPInstr, value, 1);
        for(int i = 0; i < indexList.size(); i++){
            new Use(GEPInstr, indexList.get(i), i+2);
        }
        // a[5], gep 0 -> [5 x i32]*
        // a[5][6], gep 0 -> [5 x [6 x i32]]*, gep 0 0 -> [6 x i32]*
        if(isArr){
            ArrayList<Value> arraySizeList = new ArrayList<>();
            int size1 = value.getArrayInfo().arraySize1;
            if(value.getArrayInfo().arrayDim == 1){     // a[5], gep 0 -> [5 x i32]*
                arraySizeList.add(new Constant(String.valueOf(size1), false, "int", size1));
            } else {    // a[5][6]
                int size2 = value.getArrayInfo().arraySize2;
                if(indexList.size() == 1){      // gep 0 -> [5 x [6 x i32]]*
                    arraySizeList.add(new Constant(String.valueOf(size1), false, "int", size1));
                    arraySizeList.add(new Constant(String.valueOf(size2), false, "int", size2));
                } else {    // gep 0 0 -> [6 x i32]*
                    arraySizeList.add(new Constant(String.valueOf(size2), false, "int", size2));
                }
            }
            GEPInstr.setArrayInfo(arraySizeList.size(), arraySizeList);
            GEPInstr.setDataType(getStringDataType(arraySizeList));
        }
        return GEPInstr;
    }
    // VarDef → Ident { '[' ConstExp ']' } // b
    //    | Ident { '[' ConstExp ']' } '=' InitVal // k✔
    // done
    public void VarDef(GrammarTree varDefNode, String bType) {
        ArrayList<GrammarTree> kidTreeList = varDefNode.getKidTreeList();
        VtNode identNode = (VtNode) kidTreeList.get(0);
        if (errorHandler.hasErrorB(curTable, identNode)) {        //处理错误b
            errorHandler.handleErrorB(identNode);
            return;
        }
        boolean isArray = false;
        int arrayDim = 0, i = 1;
        ArrayList<Value> arraySizeList = new ArrayList<>();
        while (i < kidTreeList.size() && kidTreeList.get(i).getVname().equals("[")) {
            isArray = true;
            arrayDim++;
            Value arraySize = ConstExp(kidTreeList.get(i + 1));
            arraySizeList.add(arraySize);
            i += 3;
        }
        GrammarTree lastNode = kidTreeList.get(kidTreeList.size() - 1);
        Token token = identNode.getToken();
        User defValue;
        if (isGlobal){        // @a = global i32 5    @c = dso_local global
            defValue = new GlobalVariable("@" + token.getTokenValue(), Type.Var, isArray, "int", false, module);
            if (isArray){
                defValue.setArrayInfo(arrayDim, arraySizeList);
                defValue.setDataType(getStringDataType(arraySizeList));
            }
        }else                // %1=alloca i32        // %1 = alloca [2 x [2 x i32]]
            defValue = allocaInstr(isArray, arraySizeList, "int");
        if(lastNode.getVname().equals("InitVal")){  // 有初始化
            ArrayList<Value> arrValueList = isArray ? new ArrayList<>() : null;
            Value initValue = InitVal(lastNode, arrValueList);
            if(isGlobal) {    // @a = global i32 5  @c = dso_local global [2 x [1 x i32]] [[1 x i32] [i32 1], [1 x i32] [i32 3]]
                if(isArray)
                    for(int j = 0; j < arrValueList.size(); j++)
                        new Use(defValue, arrValueList.get(j), j+1);
                else
                    new Use(defValue, initValue, 1);
            }
            else {            // store i32 5, i32* %1   GEP memset GEP store
                if(isArray)
                    localArrInit(arrayDim, arraySizeList, arrValueList, defValue);
                else
                    storeInstr(initValue, defValue);
            }
        } else if(isGlobal && !isArray){    // 全局变量默认初始化为0
            new Use(defValue, new Constant("0", false, "int", 0), 1);
        }
        Symbol varSymbol = new Symbol(token.getTokenValue(), bType, defValue.getName(), token.getRow(), false, isArray, arrayDim, false);
        curTable.addSymbol(varSymbol);
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    // done
    public Value InitVal(GrammarTree initValNode, ArrayList<Value> arrayValueList){
        ArrayList<GrammarTree> kidTreeList = initValNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            if(arrayValueList != null){
                arrayValueList.add(Exp(kidTreeList.get(0)));
                return null;
            }
            return Exp(kidTreeList.get(0));
        }else{
            for (GrammarTree kidTree : kidTreeList) {
                if(kidTree.getVname().equals("InitVal")){
                    InitVal(kidTree, arrayValueList);
                }
            }
        }
        return null;
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block // b g j✔
    // done
    public void FuncDef(GrammarTree funcDefNode){
        regNo = 0;
        ArrayList<GrammarTree> kidTreeList = funcDefNode.getKidTreeList();
        GrammarTree funcTypeNode = kidTreeList.get(0);
        String funcType = FuncType(funcTypeNode);
        boolean isVoid = funcType.equals("void");
        VtNode identNode = (VtNode) kidTreeList.get(1);
        if(errorHandler.hasErrorB(curTable, identNode)){        //处理错误b
            errorHandler.handleErrorB(identNode);
        }
        GrammarTree blockNode = kidTreeList.get(kidTreeList.size() - 1);
        if(!isVoid){        //判断并处理错误g
            errorHandler.checkAndHandleErrorG(blockNode);
        }
        Token identNodeToken = identNode.getToken();
        Symbol funcSymbol = new Symbol(identNodeToken.getTokenValue(), funcType, "@" + identNodeToken.getTokenValue(), identNodeToken.getRow(), false, false, 0, true);
        ArrayList<String> paramTypeList = new ArrayList<>();
        ArrayList<Integer> paramDimList = new ArrayList<>();
        SymbolTable funcTable = new SymbolTable();
        funcTable.setParentTable(curTable);
        curTable.addChildTable(funcTable);
        curTable = funcTable;

        // define i32 @foo(i32 %0, i32 %1)
        int funcParamNum = 0;
        if(kidTreeList.get(3).getVname().equals("FuncFParams"))
            funcParamNum = kidTreeList.get(3).getKidTreeList().size() / 2 + 1;
        Function function = new Function("@" + identNodeToken.getTokenValue(), funcType, module);
        curFunction = function;
        for(int i = 0; i < funcParamNum; i++){
            new Arg("%" + regNo, Type.Var, false, function, i + 1, "int");
            //System.out.println("arg regNo"+regNo);
            regNo++;
        }
        createAndSetBB();     // 先建立基本块
        if(kidTreeList.get(3).getVname().equals("FuncFParams")){
            GrammarTree funcFParamsNode = kidTreeList.get(3);
            // FuncFParams中完成形参的alloca和load
            FuncFParams(funcFParamsNode, paramTypeList, paramDimList);       // 形参加入funcTable，返回形参类型列表、维度列表
        }
        funcSymbol.setFuncInfo(paramTypeList.size(), paramTypeList, paramDimList, isVoid);
        curTable.getParentTable().addSymbol(funcSymbol);
        Block(blockNode);        // 函数体中无需再新建基本块
        curTable = curTable.getParentTable();           // 退出funcTable回到父表
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block // g j✔
    public void MainFuncDef(GrammarTree mainFuncDefNode){
        regNo = 0;
        ArrayList<GrammarTree> kidTreeList = mainFuncDefNode.getKidTreeList();
        GrammarTree blockNode = kidTreeList.get(kidTreeList.size() - 1);
        errorHandler.checkAndHandleErrorG(blockNode);        //判断并处理错误g
        VtNode mainNode = (VtNode) kidTreeList.get(1);
        curFunction = new Function("@main", "int", module);
        Symbol mainFuncSymbol = new Symbol("main", "int", "@main", mainNode.getToken().getRow(), false, false, 0, true);
        mainFuncSymbol.setFuncInfo(0, new ArrayList<>(), new ArrayList<>(), false);
        curTable.addSymbol(mainFuncSymbol);
        SymbolTable mainFuncTable = new SymbolTable();
        mainFuncTable.setParentTable(curTable);
        curTable.addChildTable(mainFuncTable);
        curTable = mainFuncTable;
        createAndSetBB();
        Block(blockNode);
        curTable = curTable.getParentTable();           // 退出funcTable回到父表
    }

    // FuncType → 'void' | 'int'
    // 返回funcType
    public String FuncType(GrammarTree funcTypeNode){
        return funcTypeNode.getKidTreeList().get(0).getVname();
    }

    // FuncFParams → FuncFParam { ',' FuncFParam }
    // 形参加入curTable，填写形参类型列表、维度列表
    // 完成形参的alloca和load
    // done
    public void FuncFParams(GrammarTree funcFParamsNode, ArrayList<String> paramTypeList, ArrayList<Integer> paramDimList){
        ArrayList<GrammarTree> kidTreeList = funcFParamsNode.getKidTreeList();
        int paramNo = 1;
        for (GrammarTree funcFParamNode : kidTreeList) {
            if (funcFParamNode.getVname().equals("FuncFParam")) {
                FuncFParam(funcFParamNode, paramTypeList, paramDimList, paramNo);      // 形参加入curTable,填写形参类型列表、维度列表
                paramNo++;
            }
        }
    }

    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]  //   b k✔
    // 形参加入curTable,填写形参类型列表、维度列表
    // done
    public void FuncFParam(GrammarTree funcFParamNode, ArrayList<String> paramTypeList, ArrayList<Integer> paramDimList, int paramNo){
        ArrayList<GrammarTree> kidTreeList = funcFParamNode.getKidTreeList();
        GrammarTree bTypeNode = kidTreeList.get(0);
        String bType = BType(bTypeNode);
        VtNode identNode = (VtNode) kidTreeList.get(1);
        if(errorHandler.hasErrorB(curTable, identNode)){        //处理错误b
            errorHandler.handleErrorB(identNode);
        }
        Token token = identNode.getToken();
        int arrayDim = 0, i = 2;
        boolean isArray = false;
        ArrayList<Value> arraySizeList = new ArrayList<>();
        while(i < kidTreeList.size() && kidTreeList.get(i).getVname().equals("[")){
            isArray = true;
            arrayDim++;
            if(kidTreeList.get(i + 1).getVname().equals("ConstExp")){
                Value constExpValue = ConstExp(kidTreeList.get(i + 1));
                arraySizeList.add(constExpValue);
            }
            i++;
        }
        Symbol paramSymbol = new Symbol(token.getTokenValue(), bType, "%" + regNo, token.getRow(), false, isArray, arrayDim, false);
        // 完成形参的alloca和store,
        Instruction allocaInstr = allocaInstr(false, null, "int");
        //System.out.println(allocaInstr);
        Arg arg = curFunction.getArgs().get(paramNo - 1);
        storeInstr(arg, allocaInstr);

        curTable.addSymbol(paramSymbol);
        paramTypeList.add(bType);
        paramDimList.add(arrayDim);
    }

    public BasicBlock createBBNoAdd2Func(){       // 创建基本块，不设为当前基本块，不加入函数
        BasicBlock bb = BasicBlock.createBBNoAdd2Func(String.valueOf(regNo), curFunction);
        regNo++;
        return bb;
    }
    public BasicBlock createAndSetBB(){     // 新建基本块并设置为当前基本块，加入函数
        curbb = new BasicBlock(String.valueOf(regNo), curFunction);
        regNo++;
        return curbb;
    }
    // Block → '{' { BlockItem } '}'
    // done
    public void Block(GrammarTree blockNode){
        ArrayList<GrammarTree> kidTreeList = blockNode.getKidTreeList();
        for (GrammarTree blockItemNode : kidTreeList) {
            if(blockItemNode.getVname().equals("BlockItem")){
                BlockItem(blockItemNode);
            }
        }
    }

    // BlockItem → Decl | Stmt
    // done
    public void BlockItem(GrammarTree blockItemNode){
        ArrayList<GrammarTree> kidTreeList = blockItemNode.getKidTreeList();
        if(kidTreeList.get(0).getVname().equals("Decl")){
            Decl(kidTreeList.get(0));
        }else{
            Stmt(kidTreeList.get(0));
        }
    }

    public Instruction loadInstr(Value src){
        Instruction loadIns = new Instruction("%" + regNo, Type.Var, false, curbb, Instruction.OPType.load, src.getDataType());
        new Use(loadIns, src, 1);
        regNo++;
        return loadIns;
    }

    public void storeInstr(Value op1, Value op2){
        Instruction storeIns = new Instruction("store", Type.Void, false, curbb, Instruction.OPType.store, "int");
        new Use(storeIns, op1, 1);
        new Use(storeIns, op2, 2);
        //System.out.println(storeIns);
    }

    // Stmt → LVal '=' Exp ';' | [Exp] ';' | Block // h i✔
    //    | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j✔
    //    | 'while' '(' Cond ')' Stmt // j✔
    //    | 'break' ';' | 'continue' ';' // i✔ m
    //    | 'return' [Exp] ';' // f i✔
    //    | LVal '=' 'getint''('')'';' // h i✔ j✔
    //    | 'printf''('FormatString{,Exp}')'';' // i✔ j✔ l
    public void Stmt(GrammarTree stmtNode){
        ArrayList<GrammarTree> kidTreeList = stmtNode.getKidTreeList();
        switch (kidTreeList.get(0).getVname()){
            case "LVal":
                GrammarTree lValNode = kidTreeList.get(0);
                ArrayList<Value> arrIndexList = new ArrayList<>();
                arrIndexList.add(new Constant("0", false, "int", 0));
                VtNode identNode = LVal(lValNode, arrIndexList);          // 返回标识符节点
                Symbol symbol = curTable.getDeclaredSymbol(identNode.getToken().getTokenValue());
                String symbolReg = symbol.getReg();
                Value symbolValue = getValue(symbolReg, curbb); // 获取标识符对应value
                assert symbolValue != null;
                if(symbolValue.isArray())
                    symbolValue = GEP(symbolValue, arrIndexList); // 获取数组元素对应value
                errorHandler.checkAndHandleErrorH(identNode, curTable);        //判断并处理错误h(修改常量值)
                if(kidTreeList.get(2).getVname().equals("Exp")) {   // LVal '=' Exp ';'
                    GrammarTree expNode = kidTreeList.get(2);
                    Value exp = Exp(expNode);
                    storeInstr(exp, symbolValue);
                } else {    // LVal '=' 'getint''('')'';'
                    Instruction instruction = new Instruction("%" + regNo, Type.Var, false, curbb, Instruction.OPType.call, "int");
                    Function getintFunc = getFunction("getint");
                    assert getintFunc != null;
                    new Use(instruction, getintFunc, 1);
                    regNo++;
                    storeInstr(instruction, symbolValue);
                }
                break;
            case "Exp":
                // done
                GrammarTree expNode = kidTreeList.get(0);
                Exp(expNode);
                break;
            case ";":
                break;
            case "Block":
                // done
                GrammarTree blockNode = kidTreeList.get(0);
                SymbolTable blockTable = new SymbolTable();
                blockTable.setParentTable(curTable);
                curTable.addChildTable(blockTable);
                curTable = blockTable;                          // 进入blockTable
                Block(blockNode);
                curTable = curTable.getParentTable();           // 退出blockTable回到父表
                break;
            case "if":    // 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j✔
                GrammarTree condNode = kidTreeList.get(2);
                BasicBlock trueBB = createBBNoAdd2Func();    // 创建trueBB、falseBB、nextBB, 但不加入函数
                BasicBlock falseBB = kidTreeList.size() == 7 ? createBBNoAdd2Func() : null;
                BasicBlock nextBB = createBBNoAdd2Func();
                Cond(condNode, trueBB, falseBB == null ? nextBB : falseBB);
                //true分支
                curbb = trueBB;
                curFunction.addBasicBlock(trueBB);
                GrammarTree stmtNode1 = kidTreeList.get(4);
                Stmt(stmtNode1);
                //false分支
                if(kidTreeList.size() == 7){
                    curbb = falseBB;
                    curFunction.addBasicBlock(falseBB);
                    GrammarTree stmtNode2 = kidTreeList.get(6);
                    Stmt(stmtNode2);
                }
                curbb = nextBB;
                curFunction.addBasicBlock(nextBB);
                if (falseBB != null) {  // trueBB to nextBB
                    Instruction stmtBr = new Instruction("br", Type.Void, false, trueBB, Instruction.OPType.br, "void");
                    new Use(stmtBr, nextBB, 1);
                }
                break;
            case "while":   // 'while' '(' Cond ')' Stmt // j✔
                BasicBlock condBB = createAndSetBB();
                BasicBlock whileBB = createBBNoAdd2Func();    // 创建whileBB、nextBB, 但不加入函数
                BasicBlock nextBB2 = createBBNoAdd2Func();
                whileCondBBStack.push(condBB);  // condBB和nextBB入栈
                whileNextBBStack.push(nextBB2);
                Cond(kidTreeList.get(2), whileBB, nextBB2);
                //while分支
                curbb = whileBB;
                curFunction.addBasicBlock(whileBB);
                cycleDepth++;
                Stmt(kidTreeList.get(4));
                cycleDepth--;
                // whileBB to condBB
                Instruction whileBr = new Instruction("br", Type.Void, false, curbb, Instruction.OPType.br, "void");
                new Use(whileBr, condBB, 1);
                curbb = nextBB2;
                curFunction.addBasicBlock(nextBB2);
                whileCondBBStack.pop();     // condBB和nextBB出栈
                whileNextBBStack.pop();
                break;
            case "break":   // 'break' ';' | 'continue' ';' // i✔ m
                BasicBlock breakBB = whileNextBBStack.peek();
                Instruction breakBr = new Instruction("br", Type.Void, false, curbb, Instruction.OPType.br, "void");
                new Use(breakBr, breakBB, 1);
                break;
            case "continue":
                if(cycleDepth == 0){
                    VtNode breakToken = (VtNode) kidTreeList.get(0);
                    errorHandler.handleErrorM(breakToken);        //处理错误m
                    break;
                }
                BasicBlock continueBB = whileCondBBStack.peek();
                Instruction continueBr = new Instruction("br", Type.Void, false, curbb, Instruction.OPType.br, "void");
                new Use(continueBr, continueBB, 1);
                break;
            case "return":  // 'return' [Exp] ';' // f i✔
                // done
                if(kidTreeList.get(1).getVname().equals("Exp")){        //存在return Exp语句
                    Value exp = Exp(kidTreeList.get(1));
                    VtNode returnNode = (VtNode) kidTreeList.get(0);
                    errorHandler.checkAndHandleErrorF(curTable, returnNode);        //判断并处理错误f,无返回值的函数存在不匹配的return语句
                    Instruction instruction = new Instruction("ret", Type.Void, false, curbb, Instruction.OPType.ret, "int");
                    new Use(instruction, exp, 1);
                } else {
                    new Instruction("ret void", Type.Void, false, curbb, Instruction.OPType.ret, "void");
                }
                break;
            case "printf":  // 'printf''('FormatString{,Exp}')'';' // i✔ j✔ l
                // done
                VtNode formatStringNode = (VtNode) kidTreeList.get(2);
                FormatString(formatStringNode);
                ArrayList<Value> expList = new ArrayList<>();
                for (int i = 3; i < kidTreeList.size(); i++) {
                    if(kidTreeList.get(i).getVname().equals("Exp")){
                        Value exp = Exp(kidTreeList.get(i));
                        expList.add(exp);
                    }
                }
                errorHandler.checkAndHandleErrorL(stmtNode);        //判断并处理错误l,格式字符与表达式个数不匹配
                String formatString = formatStringNode.getToken().getTokenValue();
                int formatStringLen = formatString.length();
                int expIndex = 0;
                for(int i = 1; i < formatStringLen - 1; i++){
                    if(formatString.charAt(i) == '%' && i+1 < formatStringLen && formatString.charAt(i + 1) == 'd'){
                        i++;
                        if(expIndex < expList.size()){
                            Value exp = expList.get(expIndex);
                            Instruction instruction = new Instruction("@putint", Type.Void, false, curbb, Instruction.OPType.call, "void");
                            Function putIntFunc = getFunction("putint");
                            assert putIntFunc != null;
                            new Use(instruction, putIntFunc, 1);
                            new Use(instruction, exp, 2);
                            expIndex++;
                        }
                    } else if(formatString.charAt(i) == '\\' && i+1 < formatStringLen && formatString.charAt(i + 1) == 'n') {
                        i++;
                        Instruction instruction = new Instruction("@putch", Type.Void, false, curbb, Instruction.OPType.call, "void");
                        Function putChFunc = getFunction("putch");
                        assert putChFunc != null;
                        new Use(instruction, putChFunc, 1);
                        int ascii = (int) '\n';
                        new Use(instruction, new Constant(String.valueOf(ascii), false, "int", ascii), 2);
                    } else {
                        Instruction instruction = new Instruction("@putch", Type.Void, false, curbb, Instruction.OPType.call, "void");
                        Function putChFunc = getFunction("putch");
                        assert putChFunc != null;
                        new Use(instruction, putChFunc, 1);
                        int ascii = formatString.charAt(i);
                        new Use(instruction, new Constant(String.valueOf(ascii), false, "int", ascii), 2);
                    }
                }
                break;
        }
    }

    // Exp → AddExp
    // done
    public Value Exp(GrammarTree expNode){
        ArrayList<GrammarTree> kidTreeList = expNode.getKidTreeList();
        return AddExp(kidTreeList.get(0));
    }

    // Cond → LOrExp
    // LOrExp → LAndExp | LOrExp '||' LAndExp
    // LAndExp → EqExp | LAndExp '&&' EqExp
    public void Cond(GrammarTree condNode, BasicBlock trueBB, BasicBlock falseBB){
        GrammarTree LOrExpNode = condNode.getKidTreeList().get(0);
        // 先将语法树改为两层list结构,第一层为LAndExp || LAndExp || LAndExp,第二层为EqExp && EqExp && EqExp, 会改变curbb
        ArrayList<ArrayList<EqExpBlock>> eqExp2LayerList = new ArrayList<>();
        LOr2EqExpBlockList(LOrExpNode, eqExp2LayerList);
        // 设置每个LAndExp的trueBB和falseBB
        for (int i = 0; i < eqExp2LayerList.size(); i++) {
            ArrayList<EqExpBlock> eqExpBlockList = eqExp2LayerList.get(i);
            for (int j = 0; j < eqExpBlockList.size(); j++) {
                EqExpBlock eqExpBlock = eqExpBlockList.get(j);
                if(i == eqExp2LayerList.size() - 1){        //最后一层的LAndExp, trueBB为传入的trueBB, falseBB为传入的falseBB
                    if(j == eqExpBlockList.size() - 1){        //最后一个LAndExp
                        eqExpBlock.setTrueBlock(trueBB);
                        eqExpBlock.setFalseBlock(falseBB);
                    } else {
                        EqExpBlock nextEq = eqExpBlockList.get(j+1);
                        eqExpBlock.setTrueBlock(nextEq.getBasicBlock()); // 1 && 2, 1为true,则跳转到2
                        eqExpBlock.setFalseBlock(falseBB);
                    }
                } else {
                    if(j == eqExpBlockList.size() - 1){        //最后一个LAndExp
                        eqExpBlock.setTrueBlock(trueBB);
                        EqExpBlock nextEq = eqExp2LayerList.get(i+1).get(0);
                        eqExpBlock.setFalseBlock(nextEq.getBasicBlock());
                    } else {
                        eqExpBlock.setTrueBlock(eqExpBlockList.get(j+1).getBasicBlock()); // 1 && 2, 1为true,则跳转到2
                        eqExpBlock.setFalseBlock(eqExp2LayerList.get(i+1).get(0).getBasicBlock());
                    }
                }
            }
        }
        // 遍历每个LAndExp, 在循环内部设置curbb
        for (ArrayList<EqExpBlock> eqExpBlockList : eqExp2LayerList) {
            for (EqExpBlock eqExpBlock : eqExpBlockList) {
                curbb = eqExpBlock.getBasicBlock();
                Value eqExp = EqExp(eqExpBlock.getEqExpNode());
                // if(2) 增加一个ne指令 2 != 0
                Instruction neIns = null;
                if (!eqExp.getDataType().equals("boolean")) {
                    neIns = new Instruction("%" + regNo, Type.Var, false, curbb, Instruction.OPType.ne, "boolean");
                    regNo++;
                    new Use(neIns, eqExp, 1);
                    new Use(neIns, new Constant("0", false, "int", 0), 2);
                }
                // 跳转指令
                Instruction br = new Instruction("br", Type.Void, false, curbb, Instruction.OPType.br, "void");
                new Use(br, neIns == null ? eqExp : neIns, 1);
                if (eqExp.isInverseCond()) {    // 是否为!a
                    new Use(br, eqExpBlock.getFalseBlock(), 2);
                    new Use(br, eqExpBlock.getTrueBlock(), 3);
                } else {
                    new Use(br, eqExpBlock.getTrueBlock(), 2);
                    new Use(br, eqExpBlock.getFalseBlock(), 3);
                }
            }
        }
        // LOrExp(kidTreeList.get(0), trueBB, falseBB, nextBB);
    }
    // LOrExp → LAndExp | LOrExp '||' LAndExp
    public void LOr2EqExpBlockList(GrammarTree LOrExpNode, ArrayList<ArrayList<EqExpBlock>> twoLayerList){
        ArrayList<GrammarTree> kidTreeList = LOrExpNode.getKidTreeList();
        ArrayList<EqExpBlock> oneLayerList = new ArrayList<>();
        twoLayerList.add(0, oneLayerList);
        if(kidTreeList.size() == 1){
            LAnd2EqExpBlockList(kidTreeList.get(0), oneLayerList);
        } else {
            LOr2EqExpBlockList(kidTreeList.get(0), twoLayerList);
            LAnd2EqExpBlockList(kidTreeList.get(2), oneLayerList);
        }
    }
    // LAndExp → EqExp | LAndExp '&&' EqExp
    public void LAnd2EqExpBlockList(GrammarTree LAndExpNode, ArrayList<EqExpBlock> eqExpBlockList){
        ArrayList<GrammarTree> kidTreeList = LAndExpNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            eqExpBlockList.add(new EqExpBlock(kidTreeList.get(0), curbb));
        } else {
            LAnd2EqExpBlockList(kidTreeList.get(0), eqExpBlockList);
            eqExpBlockList.add(new EqExpBlock(kidTreeList.get(2), curbb));
        }
        createAndSetBB();
    }

    // LVal → Ident {'[' Exp ']'} // c k✔
    // 返回Vtnode标识符节点
    public VtNode LVal(GrammarTree lValNode, ArrayList<Value> indexList){
        ArrayList<GrammarTree> kidTreeList = lValNode.getKidTreeList();
        VtNode identNode = (VtNode) kidTreeList.get(0);
        errorHandler.checkAndHandleErrorC(curTable, identNode);        //判断并处理错误c,标识符未定义
        for (int i = 1; i < kidTreeList.size(); i++) {
            if(kidTreeList.get(i).getVname().equals("Exp")){
                indexList.add(Exp(kidTreeList.get(i)));
            }
        }
        return identNode;
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number
    // done
    public Value PrimaryExp(GrammarTree primaryExpNode){
        ArrayList<GrammarTree> kidTreeList = primaryExpNode.getKidTreeList();
        GrammarTree firstNode = kidTreeList.get(0);
        switch (firstNode.getVname()){
            case "(":
                return Exp(kidTreeList.get(1));
            case "LVal":
                ArrayList<Value> arrIndexList = new ArrayList<>();
                arrIndexList.add(new Constant("0", false, "int", 0));  // gep指令的第一个参数为0
                VtNode identNode = LVal(firstNode, arrIndexList);
                Symbol symbol = curTable.getDeclaredSymbol(identNode.getToken().getTokenValue());
                String symbolReg = symbol.getReg();
                Value symbolValue = getValue(symbolReg, curbb); // 获取标识符对应value
                assert symbolValue != null;
                if(symbolValue.isArray())   // 数组元素, gep+load或global const返回常数
                    return getArrItemValue(symbolValue, arrIndexList);
                if(symbolValue.isGlobalConstant())      // const int a = 1; 则直接返回常数1
                    return ((GlobalVariable) symbolValue).getOperands().get(0);
                return loadInstr(symbolValue);
            case "Number":
                VtNode intNode = (VtNode) firstNode.getKidTreeList().get(0);
                String intValue = intNode.getToken().getTokenValue();
                return new Constant(intValue, false, "int", Integer.parseInt(intValue));
        }
        return null;
    }

    // Number → IntConst
    // 无需处理

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' // c d e j✔
    //        | UnaryOp UnaryExp
    // done
    public Value UnaryExp(GrammarTree unaryExpNode){
        ArrayList<GrammarTree> kidTreeList = unaryExpNode.getKidTreeList();
        GrammarTree firstNode = kidTreeList.get(0);
        switch (firstNode.getVname()){
            case "PrimaryExp":
                return PrimaryExp(firstNode);
            case "Ident":
                VtNode identNode = (VtNode) firstNode;
                errorHandler.checkAndHandleErrorC(curTable, identNode);             //判断并处理错误c,标识符未定义
                errorHandler.checkAndHandleErrorD(curTable, unaryExpNode);          //判断并处理错误d,函数参数个数不匹配
                errorHandler.checkAndHandleErrorE(curTable, unaryExpNode);          //判断并处理错误e,函数参数类型不匹配
                ArrayList<Value> funcRParams = new ArrayList<>();
                if(kidTreeList.get(2).getVname().equals("FuncRParams")){
                    FuncRParams(kidTreeList.get(2), funcRParams);
                }
                Function func = getFunction(identNode.getToken().getTokenValue());
                assert func != null;
                Instruction instruction;
                if(func.getDataType().equals("void")) {
                    // call void @func()
                    instruction = new Instruction(func.getName(), Type.Void, false, curbb, Instruction.OPType.call, "void");
                } else {
                    // %5 = call i32 @foo(i32 %3, i32 %4)
                    instruction = new Instruction("%" + regNo, Type.Var, false, curbb, Instruction.OPType.call, "int");
                    regNo++;
                }
                new Use(instruction, func, 1);
                int operandNo = 2;
                for (Value funcRParam : funcRParams) {
                    new Use(instruction, funcRParam, operandNo);
                    operandNo++;
                }
                return instruction;
            case "UnaryOp":
                Value value = UnaryExp(kidTreeList.get(1));
                String op = ((VtNode) firstNode.getKidTreeList().get(0)).getToken().getTokenValue();
                switch (op){
                    case "+":
                        return value;
                    case "-":
                        if(value.isConstant()){
                            Constant constant = (Constant) value;
                            int newValue = -constant.getValue();
                            String newStrValue = String.valueOf(newValue);
                            return new Constant(newStrValue, false, "int", newValue);
                        } else{
                            // %14 = sub i32 0, value
                            Constant zero = new Constant("0", false, "int", 0);
                            Instruction instr = new Instruction("%" + regNo, Type.Var, false, curbb, Instruction.OPType.sub, "int");
                            new Use(instr, zero, 1);
                            new Use(instr, value, 2);
                            regNo++;
                            return instr;
                        }
                    case "!":
                        value.inverseCond();
                        return value;
                }
                break;
        }
        return null;
    }

    // UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中
    // 无需处理

    // FuncRParams → Exp { ',' Exp }
    // done
    public void FuncRParams(GrammarTree funcRParamsNode, ArrayList<Value> funcRParams){
        ArrayList<GrammarTree> kidTreeList = funcRParamsNode.getKidTreeList();
        for (GrammarTree grammarTree : kidTreeList) {
            if (grammarTree.getVname().equals("Exp")) {
                Value exp = Exp(grammarTree);
                if(exp.isArray()){
                    ArrayList<Value> indexList = new ArrayList<>();
                    indexList.add(new Constant("0", false, "int", 0));
                    indexList.add(new Constant("0", false, "int", 0));
                    exp = GEP(exp, indexList);
                }
                funcRParams.add(exp);
            }
        }
    }

    private Value binaryOP(Value left, Value right, String op){
        boolean isIcmp = op.equals("==") || op.equals("!=") || op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=");
        if(left.isConstant() && right.isConstant()){
            Constant leftConstant = (Constant) left;
            Constant rightConstant = (Constant) right;
            int leftValue = leftConstant.getValue();
            int rightValue = rightConstant.getValue();
            int newValue = 0;
            switch (op){
                case "+":
                    newValue = leftValue + rightValue;
                    break;
                case "-":
                    newValue = leftValue - rightValue;
                    break;
                case "*":
                    newValue = leftValue * rightValue;
                    break;
                case "/":
                    newValue = leftValue / rightValue;
                    break;
                case "%":
                    newValue = leftValue % rightValue;
                    break;
                case "<":
                    newValue = leftValue < rightValue ? 1 : 0;
                    break;
                case ">":
                    newValue = leftValue > rightValue ? 1 : 0;
                    break;
                case "<=":
                    newValue = leftValue <= rightValue ? 1 : 0;
                    break;
                case ">=":
                    newValue = leftValue >= rightValue ? 1 : 0;
                    break;
                case "==":
                    newValue = leftValue == rightValue ? 1 : 0;
                    break;
                case "!=":
                    newValue = leftValue != rightValue ? 1 : 0;
                    break;
            }
            String newStrValue = String.valueOf(newValue);
            if(isIcmp){
                return new Constant(newStrValue, false, "boolean", newValue);
            }
            return new Constant(newStrValue, false, "int", newValue);
        } else{
            // %14 = icmp eq i32 %13, 0
            // %14 = sub i32 0, value
            Instruction instr = isIcmp ? new Instruction("%" + regNo, Type.Var, false, curbb, Instruction.OPType.getOpType(op), "boolean")
                                        : new Instruction("%" + regNo, Type.Var, false, curbb, Instruction.OPType.getOpType(op), "int");
            new Use(instr, left, 1);
            new Use(instr, right, 2);
            regNo++;
            return instr;
        }
    }
    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    // done
    public Value MulExp(GrammarTree mulExpNode){
        ArrayList<GrammarTree> kidTreeList = mulExpNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            return UnaryExp(kidTreeList.get(0));
        }else{
            Value mul = MulExp(kidTreeList.get(0));
            Value unary = UnaryExp(kidTreeList.get(2));
            String op = ((VtNode) kidTreeList.get(1)).getToken().getTokenValue();
            return binaryOP(mul, unary, op);
        }
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    // done
    public Value AddExp(GrammarTree addExpNode){
        ArrayList<GrammarTree> kidTreeList = addExpNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            return MulExp(kidTreeList.get(0));
        }else{
            Value add = AddExp(kidTreeList.get(0));
            Value mul = MulExp(kidTreeList.get(2));
            String op = ((VtNode) kidTreeList.get(1)).getToken().getTokenValue();
            return binaryOP(add, mul, op);
        }
    }

    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    public Value RelExp(GrammarTree relExpNode){
        ArrayList<GrammarTree> kidTreeList = relExpNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            return AddExp(kidTreeList.get(0));
        }else{
            Value rel = RelExp(kidTreeList.get(0));
            Value add = AddExp(kidTreeList.get(2));
            String op = ((VtNode) kidTreeList.get(1)).getToken().getTokenValue();
            return binaryOP(rel, add, op);
        }
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    public Value EqExp(GrammarTree eqExpNode){
        ArrayList<GrammarTree> kidTreeList = eqExpNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            return RelExp(kidTreeList.get(0));
        }else{
            Value eq = EqExp(kidTreeList.get(0));
            Value rel = RelExp(kidTreeList.get(2));
            String op = ((VtNode) kidTreeList.get(1)).getToken().getTokenValue();
            return binaryOP(eq, rel, op);
        }
    }

    // LAndExp → EqExp | LAndExp '&&' EqExp
    public void LAndExp(GrammarTree lAndExpNode, BasicBlock trueBB, BasicBlock falseBB, BasicBlock nextBB){
        ArrayList<GrammarTree> kidTreeList = lAndExpNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            EqExp(kidTreeList.get(0));
        }else{
            LAndExp(kidTreeList.get(0), trueBB, falseBB, nextBB);
            EqExp(kidTreeList.get(2));
        }
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    public void LOrExp(GrammarTree lOrExpNode, BasicBlock trueBB, BasicBlock falseBB, BasicBlock nextBB){
        ArrayList<GrammarTree> kidTreeList = lOrExpNode.getKidTreeList();
        if(kidTreeList.size() == 1){
            LAndExp(kidTreeList.get(0), trueBB, falseBB, nextBB);
        }else{
            LOrExp(kidTreeList.get(0), trueBB, falseBB, nextBB);
            LAndExp(kidTreeList.get(2), trueBB, falseBB, nextBB);
        }
    }

    // ConstExp → AddExp 注：使用的Ident 必须是常量
    // done
    public Value ConstExp(GrammarTree constExpNode){
        return AddExp(constExpNode.getKidTreeList().get(0));
    }

    // <FormatString> → '"'{<Char>}'"' // a
    public void FormatString(VtNode formatStringNode){
        errorHandler.checkAndHandleErrorA(formatStringNode);        //判断并处理错误a,格式字符串不合法
    }
}
