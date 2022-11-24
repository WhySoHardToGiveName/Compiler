import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class error_handler {
    private final ArrayList<Error> errorList = new ArrayList<>();

    // <FormatString> → '"'{<Char>}'"' // a
    // <FormatChar> → %d
    // <NormalChar> → 十进制编码为32,33,40-126的ASCII字符，'\'（编码92）出现当且仅当为'\n'
    // <Char> → <FormatChar> | <NormalChar>
    // 格式字符串中出现非法字符, 报错行号为<FormatString>所在行数
    public void checkAndHandleErrorA(VtNode formatStringNode){
        String formatString = formatStringNode.getToken().getTokenValue();
        char[] arr = formatString.toCharArray();
        int row = formatStringNode.getToken().getRow();
        for(int i = 1; i < arr.length - 1; i++){
            if(arr[i] == '%'){
                if(arr[i + 1] != 'd'){
                    System.err.println("Error: line " + row +" Format string contains illegal character.");
                    errorList.add(new Error(row, "a"));
                    return;
                }
                i++;
            } else if(arr[i] == '\\'){
                if(arr[i + 1] != 'n'){
                    System.err.println("Error: line " + row +" Format string contains illegal character.");
                    errorList.add(new Error(row, "a"));
                    return;
                }
                i++;
            } else if(arr[i] < 32 || arr[i] > 126 || (33 < arr[i] && arr[i] < 40) ){
                System.err.println("Error: line " + row +" Format string contains illegal character.");
                errorList.add(new Error(row, "a"));
                return;
            }
        }
    }

    // 函数名或者变量名在当前作用域下重复定义。注意，变量一定是同一级作用域下才会判定出错，不同级作用域下，内层会覆盖外层定义。报错行号为<Ident>所在行数
    public boolean hasErrorB(SymbolTable curTable, VtNode ident){
        String name = ident.getToken().getTokenValue();
        for(Symbol symbol : curTable.getSymbolList()){
            if(symbol.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public void handleErrorB(VtNode ident){
        String name = ident.getToken().getTokenValue();
        int row = ident.getToken().getRow();
        System.err.println("Error: line " + row + ' ' + name + " has been defined in the current scope.");
        errorList.add(new Error(row, "b"));
    }

    // 未定义的名字。报错行号为<Ident>所在行数
    public void checkAndHandleErrorC(SymbolTable curTable, VtNode ident){
        String name = ident.getToken().getTokenValue();
        int row = ident.getToken().getRow();
        Symbol declaredSymbol = curTable.getDeclaredSymbol(name);
        if(declaredSymbol != null){
            declaredSymbol.addUseRow(row);
            return;
        }
        System.err.println("Error: line " + row + ' ' + name + " is not defined.");
        errorList.add(new Error(row, "c"));
    }

    // 函数参数个数不匹配。报错行号为函数名所在行数
    // UnaryExp → Ident '(' [FuncRParams] ')'
    // FuncRParams → Exp { ',' Exp }
    public void checkAndHandleErrorD(SymbolTable curTable, GrammarTree unaryExpNode){
        ArrayList<GrammarTree> kidTreeList = unaryExpNode.getKidTreeList();
        VtNode identNode = (VtNode) kidTreeList.get(0);
        Symbol functionSymbol = curTable.getGlobalTable().getSymbol(identNode.getToken().getTokenValue()); // 函数定义在全局作用域
        if(functionSymbol != null && functionSymbol.isFunc()){
            int FParamNum = functionSymbol.getFuncInfo().paramNum;
            int RParamNum = 0;
            GrammarTree funcRParamsNode = kidTreeList.get(2);
            if(funcRParamsNode.getVname().equals("FuncRParams")){
                ArrayList<GrammarTree> funcRParamsKidTreeList = funcRParamsNode.getKidTreeList();
                for (GrammarTree funcRParamsKidTree : funcRParamsKidTreeList) {
                    if (funcRParamsKidTree.getVname().equals("Exp")) {
                        RParamNum++;
                    }
                }
            }
            if(FParamNum != RParamNum){
                int row = identNode.getToken().getRow();
                System.err.println("Error: line " + row + " the number of parameters does not match.");
                errorList.add(new Error(row, "d"));
            }
        }
    }

    // 函数参数类型不匹配，报错行号为函数名所在行数
    // UnaryExp → Ident '(' [FuncRParams] ')' // c
    // FuncRParams → Exp { ',' Exp }
    public void checkAndHandleErrorE(SymbolTable curTable, GrammarTree unaryExpNode){
        ArrayList<GrammarTree> kidTreeList = unaryExpNode.getKidTreeList();
        VtNode identNode = (VtNode) kidTreeList.get(0);
        Symbol functionSymbol = curTable.getGlobalTable().getSymbol(identNode.getToken().getTokenValue()); // 函数定义在全局作用域
        if(functionSymbol != null && functionSymbol.isFunc()){
            ArrayList<String> FParamTypeList = functionSymbol.getFuncInfo().paramTypeList;
            ArrayList<Integer> FParamArrayDimList = functionSymbol.getFuncInfo().paramArrayDimList;
            ArrayList<String> RParamTypeList = new ArrayList<>();
            ArrayList<Integer> RParamArrayDimList = new ArrayList<>();
            if(kidTreeList.get(2).getVname().equals("FuncRParams")){    // 有参数
                ArrayList<GrammarTree> funcRParamsKidTreeList = kidTreeList.get(2).getKidTreeList();
                for (GrammarTree expNode : funcRParamsKidTreeList) {
                    if (expNode.getVname().equals("Exp")) {
                        ArrayList<GrammarTree> identKidTreeList = expNode.getIdentKidTreeList();     // 可能为null
                        if(identKidTreeList != null) {       // 存在调用Ident作为实参,两种情况LVal → Ident {'[' Exp ']'}或UnaryExp → Ident '(' [FuncRParams] ')'
                            VtNode RParamIdentNode = (VtNode) identKidTreeList.get(0);
                            Symbol RParamSymbol = curTable.getDeclaredSymbol(RParamIdentNode.getToken().getTokenValue());
                            if(RParamSymbol == null){       // 未定义的名字,留到后面处理
                                return;
                            }
                            int RParamArrayDim = 0;     // 实参使用的数组维数
                            for (GrammarTree identBrotherNode : identKidTreeList) {
                                if (identBrotherNode.getVname().equals("Exp")) {
                                    RParamArrayDim++;
                                }
                            }
                            RParamTypeList.add(RParamSymbol.getType());
                            RParamArrayDimList.add(RParamSymbol.getArrayDim() - RParamArrayDim);
                        } else {            // 无Ident，只有常量
                            RParamTypeList.add("int");
                            RParamArrayDimList.add(0);
                        }
                    }
                }
            }
            if(FParamArrayDimList.size() != RParamArrayDimList.size() || FParamTypeList.size() != RParamTypeList.size()){
                return;
            }
            for(int i = 0; i < FParamTypeList.size(); i++){
                if(!FParamTypeList.get(i).equals(RParamTypeList.get(i)) || !FParamArrayDimList.get(i).equals(RParamArrayDimList.get(i))){
                    int row = identNode.getToken().getRow();
                    String name = identNode.getToken().getTokenValue();
                    System.err.println("Error: line " + row + " the type of parameter in function " + name + " does not match.");
                    errorList.add(new Error(row, "e"));
                    return;
                }
            }
        }
    }

    // 已发现return Exp
    // f: 无返回值的函数存在不匹配的return语句。报错行号为<Return>所在行数
    // 判断当前table的所有父表的函数是否有返回值，如果无返回值就报错
    public void checkAndHandleErrorF(SymbolTable curTable, VtNode returnNode){
        SymbolTable symbolTable = curTable;
        while(symbolTable.getParentTable() != null){
            symbolTable = symbolTable.getParentTable();
            Symbol topSymbol = symbolTable.getTopSymbol();
            if(topSymbol != null && topSymbol.isFunc()){                     // 找到函数
                if(topSymbol.getFuncInfo().isVoid){     // 是void函数
                    System.err.println("Error: line " + returnNode.getToken().getRow() + " invalid return statement in a void function.");
                    errorList.add(new Error(returnNode.getToken().getRow(), "f"));
                } else {
                    return;
                }
            }
        }
    }

    // 有返回值的函数缺少return语句
    // Block → '{' { BlockItem } '}'
    // BlockItem → Decl | Stmt
    // Stmt → 'return' [Exp] ';' | ...
    public void checkAndHandleErrorG(GrammarTree blockNode){
        // 找到最后一个BlockItem
        ArrayList<GrammarTree> kidTreeList = blockNode.getKidTreeList();
        int size = kidTreeList.size();
        boolean hasReturn = false;
        VtNode RBraceNode = (VtNode) kidTreeList.get(size - 1);
        GrammarTree blockItemNode = kidTreeList.get(size - 2);
        if(blockItemNode.getVname().equals("BlockItem")){
            GrammarTree stmtNode = blockItemNode.getKidTreeList().get(0);
            if(stmtNode.getVname().equals("Stmt")){
                VtNode returnNode = (VtNode) stmtNode.getKidTreeList().get(0);
                if(returnNode.getToken().getTokenValue().equals("return")){
                    hasReturn = true;
                }
            }
        }
        if(!hasReturn){
            int row = RBraceNode.getToken().getRow();
            System.err.println("Error: line " + row + " missing return statement.");
            errorList.add(new Error(row, "g"));
        }
    }

    // 不能改变常量的值,报错行号为<LVal>所在行号。
    public void checkAndHandleErrorH(VtNode identNode, SymbolTable curTable){
        String name = identNode.getToken().getTokenValue();
        Symbol symbol = curTable.getDeclaredSymbol(name);
        if(symbol != null && symbol.isConst()){
            int row = identNode.getToken().getRow();
            System.err.println("Error: line " + row + " cannot change the value of constant " + name + ".");
            errorList.add(new Error(row, "h"));
        }
    }

    // IJK:缺少 ; ) ]
    public void handleErrorIJK(Token lastToken, GrammarTree node, String errorCode){
        String tokenValue = null;
        String tokenCode;
        switch (errorCode){
            case "i":
                tokenValue = ";";
                break;
            case "j":
                tokenValue = ")";
                break;
            case "k":
                tokenValue = "]";
                break;
        }
        tokenCode = TokenMap.getTokenCode(tokenValue);
        System.err.println("Error: line " + lastToken.getRow() + " expected " + tokenValue + " after " + lastToken.getTokenValue());
        this.errorList.add(new Error(lastToken.getRow(), errorCode));
        Token token = new Token(tokenCode, tokenValue, lastToken.getRow());
        node.insertKidTree(new VtNode(tokenValue, token));
    }

    // 格式字符与表达式个数不匹配
    // Stmt → 'printf''('FormatString{','Exp}')'';'
    // 报错行号为‘printf’所在行号
    public void checkAndHandleErrorL(GrammarTree printfStmtNode){
        ArrayList<GrammarTree> kidTreeList = printfStmtNode.getKidTreeList();
        VtNode formatStringNode = (VtNode) kidTreeList.get(2);
        String formatString = formatStringNode.getToken().getTokenValue();
        int formatStringLen = formatString.length();
        int formatCharNum = 0, expNum = 0;
        for(int i = 0; i < formatStringLen; i++){
            if(formatString.charAt(i) == '%' && i+1 < formatStringLen && formatString.charAt(i + 1) == 'd'){
                formatCharNum++;
            }
        }
        for(int i = 3; i < kidTreeList.size(); i ++){
            if(kidTreeList.get(i).getVname().equals("Exp")){
                expNum++;
            }
        }
        if(formatCharNum != expNum){
            int row = ((VtNode) kidTreeList.get(0)).getToken().getRow();
            System.err.println("Error: line " + row + " FormatString and expression number do not match.");
            errorList.add(new Error(row, "l"));
        }
    }

    // 在非循环块中使用break和continue语句，报错行号为<Break>或<Continue>所在行号
    public void handleErrorM(VtNode breakContinueNode){
        Token token = breakContinueNode.getToken();
        System.err.println("Error: line " + token.getRow() + " " + token.getTokenValue() + " is not allowed in this scope.");
        errorList.add(new Error(token.getRow(), "m"));
    }

    public void printErrorList(String outputPath) throws IOException {
        //先根据行号排序
        errorList.sort(Comparator.comparingInt(Error::getRow));
        FileWriter fw = new FileWriter(outputPath);
        BufferedWriter bw = new BufferedWriter(fw);
        for(Error error : errorList){
            bw.write(error.toString());
            bw.newLine();
        }
        bw.close();
        fw.close();
    }
}
