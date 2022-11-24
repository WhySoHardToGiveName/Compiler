import java.util.ArrayList;

public class syntactic_analyzer {
    private final ArrayList<Token> tokenList;
    private int curIndex = 0;
    private final error_handler errorHandler;

    public syntactic_analyzer(ArrayList<Token> tokenList, error_handler errorHandler) {
        this.tokenList = tokenList;
        this.errorHandler = errorHandler;
    }

    private Token getCurToken(){
        return tokenList.get(curIndex);
    }
    private void nextSymbol(){
        curIndex++;
    }
    private Token getLastToken(){
        return tokenList.get(curIndex - 1);
    }
    private boolean curSymbolIs(String symbolCode){
        return getCurToken().getTokenCode().equals(symbolCode);
    }
    private boolean futureSymbolIs(String symbolCode, int offset){
        if(curIndex + offset >= tokenList.size()){
            return false;
        }
        return tokenList.get(curIndex + offset).getTokenCode().equals(symbolCode);
    }
    private boolean hasAssignBeforeSemicn(){
        int offset = 0;
        while(curIndex + offset < tokenList.size()){
            if(futureSymbolIs("ASSIGN", offset)){
                return true;
            }
            if(futureSymbolIs("SEMICN", offset)){
                return false;
            }
            offset++;
        }
        return false;
    }
    private boolean hasRParentBeforeSemicn(){
        int offset = 0;
        while(curIndex + offset < tokenList.size()){
            if(futureSymbolIs("RPARENT", offset)){
                return true;
            }
            if(futureSymbolIs("SEMICN", offset)){
                return false;
            }
            offset++;
        }
        return false;
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public GrammarTree CompUnit(){
        GrammarTree compUnitTree = new GrammarTree("CompUnit", false);
        while(curSymbolIs("CONSTTK") ||
                (curSymbolIs("INTTK") && futureSymbolIs("IDENFR", 1) && !futureSymbolIs("LPARENT", 2))){
            compUnitTree.insertKidTree(Decl());
        }
        while(curSymbolIs("VOIDTK") ||
                (curSymbolIs("INTTK") && futureSymbolIs("IDENFR", 1) && futureSymbolIs("LPARENT", 2))){
            compUnitTree.insertKidTree(FuncDef());
        }
        compUnitTree.insertKidTree(MainFuncDef());
        System.out.println("syntax analysis done!");
        return compUnitTree;
    }

    // Decl → ConstDecl | VarDecl
    private GrammarTree Decl(){
        GrammarTree declTree = new GrammarTree("Decl", false);
        if(curSymbolIs("CONSTTK")){
            declTree.insertKidTree(ConstDecl());
        } else if(curSymbolIs("INTTK")){
            declTree.insertKidTree(VarDecl());
        } else{
            System.err.println("Error: Decl has no ConstDecl or VarDecl");
            return null;
        }
        return declTree;
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private GrammarTree ConstDecl(){
        if(!curSymbolIs("CONSTTK")){
            System.err.println("Error: ConstDecl begins without 'const'");
            return null;
        }
        GrammarTree constDeclTree = new GrammarTree("ConstDecl", false);
        constDeclTree.insertKidTree(new VtNode("const", getCurToken()));
        nextSymbol();
        constDeclTree.insertKidTree(BType());
        constDeclTree.insertKidTree(ConstDef());
        while(curSymbolIs("COMMA")){
            constDeclTree.insertKidTree(new VtNode(",", getCurToken()));
            nextSymbol();
            constDeclTree.insertKidTree(ConstDef());
        }
        if(!curSymbolIs("SEMICN")){
            Token lastToken = getLastToken();
            errorHandler.handleErrorIJK(lastToken, constDeclTree, "i");     // 缺少 ;
            return constDeclTree;
        }
        constDeclTree.insertKidTree(new VtNode(";", getCurToken()));
        nextSymbol();
        return constDeclTree;
    }

    // BType → 'int'
    private GrammarTree BType(){
        if(!curSymbolIs("INTTK")){
            System.err.println("Error: BType begins without 'int'");
            return null;
        }
        GrammarTree bTypeTree = new GrammarTree("BType", false);
        bTypeTree.insertKidTree(new VtNode("int", getCurToken()));
        nextSymbol();
        return bTypeTree;
    }

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    private GrammarTree ConstDef(){
        GrammarTree constDefTree = new GrammarTree("ConstDef", false);
        if(!curSymbolIs("IDENFR")){
            System.err.println("Error: ConstDef begins without Ident");
            return null;
        }
        constDefTree.insertKidTree(new VtNode("Ident", getCurToken()));
        nextSymbol();
        while(curSymbolIs("LBRACK")){
            constDefTree.insertKidTree(new VtNode("[", getCurToken()));
            nextSymbol();
            constDefTree.insertKidTree(ConstExp());
            if(!curSymbolIs("RBRACK")){
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, constDefTree, "k");      // 缺少 ]
                continue;
            }
            constDefTree.insertKidTree(new VtNode("]", getCurToken()));
            nextSymbol();
        }
        if(!curSymbolIs("ASSIGN")){
            System.err.println("Error: ConstDef has no '='");
            return null;
        }
        constDefTree.insertKidTree(new VtNode("=", getCurToken()));
        nextSymbol();
        constDefTree.insertKidTree(ConstInitVal());
        return constDefTree;
    }

    // ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    // First(ConstExp) = { '+', '-', '(', 'IDENFR', 'INTCON' }
    private GrammarTree ConstInitVal(){
        GrammarTree constInitValTree = new GrammarTree("ConstInitVal", false);
        if(!curSymbolIs("LBRACE")){
            constInitValTree.insertKidTree(ConstExp());
        } else{
            constInitValTree.insertKidTree(new VtNode("{", getCurToken()));
            nextSymbol();
            if(curSymbolIs("LBRACE") || curSymbolIs("PLUS") || curSymbolIs("MINU") ||
                    curSymbolIs("LPARENT") || curSymbolIs("IDENFR") || curSymbolIs("INTCON")){
                constInitValTree.insertKidTree(ConstInitVal());
                while(curSymbolIs("COMMA")){
                    constInitValTree.insertKidTree(new VtNode(",", getCurToken()));
                    nextSymbol();
                    constInitValTree.insertKidTree(ConstInitVal());
                }
            }
            if(!curSymbolIs("RBRACE")){
                System.err.println("Error: ConstInitVal has '{' but no '}'");
                return null;
            }
            constInitValTree.insertKidTree(new VtNode("}", getCurToken()));
            nextSymbol();
        }
        return constInitValTree;
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    private GrammarTree VarDecl(){
        if(!curSymbolIs("INTTK")){
            System.err.println("Error: VarDecl begins without 'int'");
            return null;
        }
        GrammarTree varDeclTree = new GrammarTree("VarDecl", false);
        varDeclTree.insertKidTree(BType());
        varDeclTree.insertKidTree(VarDef());
        while(curSymbolIs("COMMA")){
            varDeclTree.insertKidTree(new VtNode(",", getCurToken()));
            nextSymbol();
            varDeclTree.insertKidTree(VarDef());
        }
        if(!curSymbolIs("SEMICN")){
            Token lastToken = getLastToken();
            errorHandler.handleErrorIJK(lastToken, varDeclTree, "i");     // 缺少 ;
            return varDeclTree;
        }
        varDeclTree.insertKidTree(new VtNode(";", getCurToken()));
        nextSymbol();
        return varDeclTree;
    }

    // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal 包含普通变量、一维数组、二维数组定义
    private GrammarTree VarDef(){
        GrammarTree varDefTree = new GrammarTree("VarDef", false);
        if(!curSymbolIs("IDENFR")){
            System.err.println("Error: VarDef begins without Ident");
            return null;
        }
        varDefTree.insertKidTree(new VtNode("Ident", getCurToken()));
        nextSymbol();
        while(curSymbolIs("LBRACK")){
            varDefTree.insertKidTree(new VtNode("[", getCurToken()));
            nextSymbol();
            varDefTree.insertKidTree(ConstExp());
            if(!curSymbolIs("RBRACK")){
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, varDefTree, "k");      // 缺少 ]
                continue;
            }
            varDefTree.insertKidTree(new VtNode("]", getCurToken()));
            nextSymbol();
        }
        if(curSymbolIs("ASSIGN")){
            varDefTree.insertKidTree(new VtNode("=", getCurToken()));
            nextSymbol();
            varDefTree.insertKidTree(InitVal());
        }
        return varDefTree;
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    // First(Exp) = { '+', '-', '(', 'IDENFR', 'INTCON' }
    private GrammarTree InitVal(){
        GrammarTree initValTree = new GrammarTree("InitVal", false);
        if(!curSymbolIs("LBRACE")){
            initValTree.insertKidTree(Exp());
        } else{
            initValTree.insertKidTree(new VtNode("{", getCurToken()));
            nextSymbol();
            if(curSymbolIs("LBRACE") || curSymbolIs("PLUS") || curSymbolIs("MINU") ||
                    curSymbolIs("LPARENT") || curSymbolIs("IDENFR") || curSymbolIs("INTCON")){
                initValTree.insertKidTree(InitVal());
                while(curSymbolIs("COMMA")){
                    initValTree.insertKidTree(new VtNode(",", getCurToken()));
                    nextSymbol();
                    initValTree.insertKidTree(InitVal());
                }
            }
            if(!curSymbolIs("RBRACE")){
                System.err.println("Error: InitVal has '{' but no '}'");
                return null;
            }
            initValTree.insertKidTree(new VtNode("}", getCurToken()));
            nextSymbol();
        }
        return initValTree;
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    private GrammarTree FuncDef(){
        GrammarTree funcDefTree = new GrammarTree("FuncDef", false);
        funcDefTree.insertKidTree(FuncType());
        if(!curSymbolIs("IDENFR")){
            System.err.println("Error: FuncDef begins without Ident");
            return null;
        }
        funcDefTree.insertKidTree(new VtNode("Ident", getCurToken()));
        nextSymbol();
        if(!curSymbolIs("LPARENT")){
            System.err.println("Error: FuncDef has no '('");
            return null;
        }
        funcDefTree.insertKidTree(new VtNode("(", getCurToken()));
        nextSymbol();
        if(curSymbolIs("INTTK")){
            funcDefTree.insertKidTree(FuncFParams());
        }
        if(!curSymbolIs("RPARENT")){
            Token lastToken = getLastToken();
            errorHandler.handleErrorIJK(lastToken, funcDefTree, "j");     // 缺少 )
            funcDefTree.insertKidTree(Block());
            return funcDefTree;
        }
        funcDefTree.insertKidTree(new VtNode(")", getCurToken()));
        nextSymbol();
        funcDefTree.insertKidTree(Block());
        return funcDefTree;
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    private GrammarTree MainFuncDef(){
        GrammarTree mainFuncDefTree = new GrammarTree("MainFuncDef", false);
        if(!curSymbolIs("INTTK")){
            System.err.println("Error: MainFuncDef begins without 'int'");
            return null;
        }
        mainFuncDefTree.insertKidTree(new VtNode("int", getCurToken()));
        nextSymbol();
        if(!curSymbolIs("MAINTK")){
            System.err.println("Error: MainFuncDef has no 'main'");
            return null;
        }
        mainFuncDefTree.insertKidTree(new VtNode("main", getCurToken()));
        nextSymbol();
        if(!curSymbolIs("LPARENT")){
            System.err.println("Error: MainFuncDef has no '('");
            return null;
        }
        mainFuncDefTree.insertKidTree(new VtNode("(", getCurToken()));
        nextSymbol();
        if(!curSymbolIs("RPARENT")){
            Token lastToken = getLastToken();
            errorHandler.handleErrorIJK(lastToken, mainFuncDefTree, "j");     // 缺少 )
            mainFuncDefTree.insertKidTree(Block());
            return mainFuncDefTree;
        }
        mainFuncDefTree.insertKidTree(new VtNode(")", getCurToken()));
        nextSymbol();
        mainFuncDefTree.insertKidTree(Block());
        return mainFuncDefTree;
    }

    // FuncType → 'void' | 'int'
    private GrammarTree FuncType(){
        GrammarTree funcTypeTree = new GrammarTree("FuncType", false);
        if(curSymbolIs("VOIDTK")){
            funcTypeTree.insertKidTree(new VtNode("void", getCurToken()));
            nextSymbol();
        } else if(curSymbolIs("INTTK")){
            funcTypeTree.insertKidTree(new VtNode("int", getCurToken()));
            nextSymbol();
        } else{
            System.err.println("Error: FuncType is not 'void' or 'int'");
            return null;
        }
        return funcTypeTree;
    }

    // FuncFParams → FuncFParam { ',' FuncFParam }
    private GrammarTree FuncFParams(){
        GrammarTree funcFParamsTree = new GrammarTree("FuncFParams", false);
        funcFParamsTree.insertKidTree(FuncFParam());
        while(curSymbolIs("COMMA")){
            funcFParamsTree.insertKidTree(new VtNode(",", getCurToken()));
            nextSymbol();
            funcFParamsTree.insertKidTree(FuncFParam());
        }
        return funcFParamsTree;
    }

    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    private GrammarTree FuncFParam(){
        GrammarTree funcFParamTree = new GrammarTree("FuncFParam", false);
        funcFParamTree.insertKidTree(BType());
        if(!curSymbolIs("IDENFR")){
            System.err.println("Error: FuncFParam has no Ident");
            return null;
        }
        funcFParamTree.insertKidTree(new VtNode("Ident", getCurToken()));
        nextSymbol();
        if(curSymbolIs("LBRACK")){
            funcFParamTree.insertKidTree(new VtNode("[", getCurToken()));
            nextSymbol();
            if(!curSymbolIs("RBRACK")){
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, funcFParamTree, "k");     // 缺少 ]
            } else {
                funcFParamTree.insertKidTree(new VtNode("]", getCurToken()));
                nextSymbol();
            }
            while(curSymbolIs("LBRACK")){
                funcFParamTree.insertKidTree(new VtNode("[", getCurToken()));
                nextSymbol();
                funcFParamTree.insertKidTree(ConstExp());
                if(!curSymbolIs("RBRACK")){
                    System.err.println("Error: FuncFParam has '[' but no ']'");
                    return null;
                }
                funcFParamTree.insertKidTree(new VtNode("]", getCurToken()));
                nextSymbol();
            }
        }
        return funcFParamTree;
    }

    // Block → '{' { BlockItem } '}'
    private GrammarTree Block(){
        GrammarTree blockTree = new GrammarTree("Block", false);
        if(!curSymbolIs("LBRACE")){
            System.err.println("Error: Block has no '{'");
            return null;
        }
        blockTree.insertKidTree(new VtNode("{", getCurToken()));
        nextSymbol();
        while(!curSymbolIs("RBRACE")){
            blockTree.insertKidTree(BlockItem());
        }
        if(!curSymbolIs("RBRACE")){
            System.err.println("Error: Block has '{' but no '}'");
            return null;
        }
        blockTree.insertKidTree(new VtNode("}", getCurToken()));
        nextSymbol();
        return blockTree;
    }

    // BlockItem → Decl | Stmt
    private GrammarTree BlockItem(){
        GrammarTree blockItemTree = new GrammarTree("BlockItem", false);
        if(curSymbolIs("CONSTTK") || curSymbolIs("INTTK")){
            blockItemTree.insertKidTree(Decl());
        } else{
            blockItemTree.insertKidTree(Stmt());
        }
        return blockItemTree;
    }

    /*
    Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
    | [Exp] ';' //有无Exp两种情况
    | Block  // → '{' { BlockItem } '}'
    | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
    | 'while' '(' Cond ')' Stmt
    | 'break' ';' | 'continue' ';'
    | 'return' [Exp] ';' // 1.有Exp 2.无Exp
    | LVal '=' 'getint''('')'';'
    | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
    */
    private GrammarTree Stmt() {
        GrammarTree stmtTree = new GrammarTree("Stmt", false);
        if (curSymbolIs("LBRACE")) {      // Block
            stmtTree.insertKidTree(Block());
        } else if (curSymbolIs("IFTK")) {    // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            stmtTree.insertKidTree(new VtNode("if", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("LPARENT")) {
                System.err.println("Error: Stmt has 'if' but no '('");
                return null;
            }
            stmtTree.insertKidTree(new VtNode("(", getCurToken()));
            nextSymbol();
            stmtTree.insertKidTree(Cond());
            if (!curSymbolIs("RPARENT")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "j");     // 缺少 )
            } else {
                stmtTree.insertKidTree(new VtNode(")", getCurToken()));
                nextSymbol();
            }
            stmtTree.insertKidTree(Stmt());
            if (curSymbolIs("ELSETK")) {
                stmtTree.insertKidTree(new VtNode("else", getCurToken()));
                nextSymbol();
                stmtTree.insertKidTree(Stmt());
            }
        } else if (curSymbolIs("WHILETK")) {   // 'while' '(' Cond ')' Stmt
            stmtTree.insertKidTree(new VtNode("while", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("LPARENT")) {
                System.err.println("Error: Stmt has 'while' but no '('");
                return null;
            }
            stmtTree.insertKidTree(new VtNode("(", getCurToken()));
            nextSymbol();
            stmtTree.insertKidTree(Cond());
            if (!curSymbolIs("RPARENT")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "j");     // 缺少 )
            } else {
                stmtTree.insertKidTree(new VtNode(")", getCurToken()));
                nextSymbol();
            }
            stmtTree.insertKidTree(Stmt());
        } else if (curSymbolIs("BREAKTK")) {      // 'break' ';'
            stmtTree.insertKidTree(new VtNode("break", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("SEMICN")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
            } else {
                stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                nextSymbol();
            }
        } else if (curSymbolIs("CONTINUETK")) {       // 'continue' ';'
            stmtTree.insertKidTree(new VtNode("continue", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("SEMICN")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
            } else {
                stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                nextSymbol();
            }
        } else if (curSymbolIs("RETURNTK")) {     // 'return' [Exp] ';'
            stmtTree.insertKidTree(new VtNode("return", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("SEMICN")) {       // 有Exp或漏了分号
                // Exp的first集为IDENFR、LPARENT、PLUS、MINU、INTCON
                if (curSymbolIs("IDENFR") || curSymbolIs("LPARENT") || curSymbolIs("PLUS") || curSymbolIs("MINU") || curSymbolIs("INTCON")) {
                    stmtTree.insertKidTree(Exp());
                    if (!curSymbolIs("SEMICN")) {
                        Token lastToken = getLastToken();
                        errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
                    } else {
                        stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                        nextSymbol();
                    }
                } else {
                    Token lastToken = getLastToken();
                    errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
                }
                /*stmtTree.insertKidTree(Exp());
                if (!curSymbolIs("SEMICN")) {
                    Token lastToken = getLastToken();
                    errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
                } else {
                    stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                    nextSymbol();
                }*/
            } else {
                stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                nextSymbol();
            }
        } else if (curSymbolIs("PRINTFTK")) {     // 'printf''('FormatString{','Exp}')'';'
            stmtTree.insertKidTree(new VtNode("printf", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("LPARENT")) {
                System.err.println("Error: Stmt has 'printf' but no '('");
                return null;
            }
            stmtTree.insertKidTree(new VtNode("(", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("STRCON")) {
                System.err.println("Error: Stmt has printf but no FormatString");
                return null;
            }
            stmtTree.insertKidTree(new VtNode(getCurToken().getTokenValue(), getCurToken()));
            nextSymbol();
            while (curSymbolIs("COMMA")) {
                stmtTree.insertKidTree(new VtNode(",", getCurToken()));
                nextSymbol();
                stmtTree.insertKidTree(Exp());
            }
            if (!curSymbolIs("RPARENT")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "j");     // 缺少 )
            } else {
                stmtTree.insertKidTree(new VtNode(")", getCurToken()));
                nextSymbol();
            }
            if (!curSymbolIs("SEMICN")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
            } else {
                stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                nextSymbol();
            }
        } else if (hasAssignBeforeSemicn()) {     // LVal '=' 'getint''('')'';' | LVal '=' Exp ';'
            stmtTree.insertKidTree(LVal());
            if (!curSymbolIs("ASSIGN")) {
                System.err.println("Stmt" + getCurToken().getTokenCode() + ' ' + getCurToken().getTokenValue());
                System.err.println("Error: Stmt has LVal but no '='");
                System.exit(1);
                return null;
            }
            stmtTree.insertKidTree(new VtNode("=", getCurToken()));
            nextSymbol();
            if (curSymbolIs("GETINTTK")) {    // LVal '=' 'getint''('')'';'
                stmtTree.insertKidTree(new VtNode("getint", getCurToken()));
                nextSymbol();
                if (!curSymbolIs("LPARENT")) {
                    System.err.println("Error: Stmt has 'getint' but no '('");
                    return null;
                }
                stmtTree.insertKidTree(new VtNode("(", getCurToken()));
                nextSymbol();
                if (!curSymbolIs("RPARENT")) {
                    Token lastToken = getLastToken();
                    errorHandler.handleErrorIJK(lastToken, stmtTree, "j");     // 缺少 )
                } else {
                    stmtTree.insertKidTree(new VtNode(")", getCurToken()));
                    nextSymbol();
                }
            } else {                            // LVal '=' Exp ';'
                stmtTree.insertKidTree(Exp());
            }
            if (!curSymbolIs("SEMICN")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
            } else {
                stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                nextSymbol();
            }
        } else {                 // [Exp] ';'
            if (!curSymbolIs("SEMICN")) {
                stmtTree.insertKidTree(Exp());
            }
            if (!curSymbolIs("SEMICN")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, stmtTree, "i");     // 缺少 ;
            } else {
                stmtTree.insertKidTree(new VtNode(";", getCurToken()));
                nextSymbol();
            }
        }
        return stmtTree;
    }

    // Exp → AddExp
    private GrammarTree Exp() {
        GrammarTree expTree = new GrammarTree("Exp", false);
        expTree.insertKidTree(AddExp());
        return expTree;
    }

    // Cond → LOrExp
    private GrammarTree Cond() {
        GrammarTree condTree = new GrammarTree("Cond", false);
        condTree.insertKidTree(LOrExp());
        return condTree;
    }

    // LVal → Ident {'[' Exp ']'}
    private GrammarTree LVal() {
        GrammarTree lvalTree = new GrammarTree("LVal", false);
        if(!curSymbolIs("IDENFR")) {
            System.err.println("LVal line" +getCurToken().getRow() + ' ' + getCurToken().getTokenCode() + ' ' + getCurToken().getTokenValue());
            System.err.println("Error: LVal begins without Ident");
            System.exit(1);
            return null;
        }
        lvalTree.insertKidTree(new VtNode("Ident", getCurToken()));
        nextSymbol();
        while (curSymbolIs("LBRACK")) {
            lvalTree.insertKidTree(new VtNode("[", getCurToken()));
            nextSymbol();
            lvalTree.insertKidTree(Exp());
            if (!curSymbolIs("RBRACK")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, lvalTree, "k");     // 缺少 ]
            } else {
                lvalTree.insertKidTree(new VtNode("]", getCurToken()));
                nextSymbol();
            }
        }
        return lvalTree;
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number
    private GrammarTree PrimaryExp() {
        GrammarTree primaryExpTree = new GrammarTree("PrimaryExp", false);
        if (curSymbolIs("LPARENT")) {    // '(' Exp ')'
            primaryExpTree.insertKidTree(new VtNode("(", getCurToken()));
            nextSymbol();
            primaryExpTree.insertKidTree(Exp());
            if (!curSymbolIs("RPARENT")) {
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, primaryExpTree, "j");     // 缺少 )
            } else {
                primaryExpTree.insertKidTree(new VtNode(")", getCurToken()));
                nextSymbol();
            }
        } else if (curSymbolIs("IDENFR")) {    // LVal
            primaryExpTree.insertKidTree(LVal());
        } else if (curSymbolIs("INTCON")) {    // Number
            primaryExpTree.insertKidTree(Number());
        } else {
            System.err.println("Error: PrimaryExp begins without '(' or Ident or Number");
            System.err.println(getCurToken().getTokenValue() + ' ' + getCurToken().getRow());
            System.exit(1);
            return null;
        }
        return primaryExpTree;
    }

    // Number → IntConst
    private GrammarTree Number() {
        GrammarTree numberTree = new GrammarTree("Number", false);
        numberTree.insertKidTree(new VtNode("IntConst", getCurToken()));
        nextSymbol();
        return numberTree;
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp
    // PrimaryExp → '(' Exp ')' | LVal → Ident {'[' Exp ']'} | Number → IntConst
    // UnaryOp → '+' | '−' | '!'
    private GrammarTree UnaryExp() {
        GrammarTree unaryExpTree = new GrammarTree("UnaryExp", false);
        if (curSymbolIs("IDENFR") && futureSymbolIs("LPARENT", 1)) {    // Ident '(' [FuncRParams] ')'
            unaryExpTree.insertKidTree(new VtNode("Ident", getCurToken()));
            nextSymbol();
            if (!curSymbolIs("LPARENT")) {
                System.err.println("Error: UnaryExp has Ident but no '('");
                return null;
            }
            unaryExpTree.insertKidTree(new VtNode("(", getCurToken()));
            nextSymbol();
            // FuncRParams的first集合为{IDENFR, IntConst, LPARENT, MINU, PLUS}
            if (curSymbolIs("IDENFR") || curSymbolIs("INTCON") || curSymbolIs("LPARENT") || curSymbolIs("PLUS") || curSymbolIs("MINU")) {
                unaryExpTree.insertKidTree(FuncRParams());
                if(!curSymbolIs("RPARENT")) {
                    Token lastToken = getLastToken();
                    errorHandler.handleErrorIJK(lastToken, unaryExpTree, "j");     // 缺少 )
                } else {
                    unaryExpTree.insertKidTree(new VtNode(")", getCurToken()));
                    nextSymbol();
                }
            } else if(curSymbolIs("RPARENT")) {     // Ident '(' ')'
                unaryExpTree.insertKidTree(new VtNode(")", getCurToken()));
                nextSymbol();
            } else {    // Ident '(' ')'漏右括号
                Token lastToken = getLastToken();
                errorHandler.handleErrorIJK(lastToken, unaryExpTree, "j");     // 缺少 )
            }
        } else if (curSymbolIs("PLUS") || curSymbolIs("MINU") || curSymbolIs("NOT")) {    // UnaryOp UnaryExp
            unaryExpTree.insertKidTree(UnaryOp());
            unaryExpTree.insertKidTree(UnaryExp());
        } else {
            unaryExpTree.insertKidTree(PrimaryExp());       // PrimaryExp → '(' Exp ')' | LVal → Ident {'[' Exp ']'} | Number → IntConst
        }
        return unaryExpTree;
    }

    // UnaryOp → '+' | '−' | '!'
    private GrammarTree UnaryOp() {
        GrammarTree unaryOpTree = new GrammarTree("UnaryOp", false);
        if (curSymbolIs("PLUS")) {
            unaryOpTree.insertKidTree(new VtNode("+", getCurToken()));
        } else if (curSymbolIs("MINU")) {
            unaryOpTree.insertKidTree(new VtNode("-", getCurToken()));
        } else if (curSymbolIs("NOT")) {
            unaryOpTree.insertKidTree(new VtNode("!", getCurToken()));
        } else {
            System.err.println("Error: UnaryOp begins without '+' or '-' or '!'");
            return null;
        }
        nextSymbol();
        return unaryOpTree;
    }

    // FuncRParams → Exp { ',' Exp }
    private GrammarTree FuncRParams() {
        GrammarTree funcRParamsTree = new GrammarTree("FuncRParams", false);
        funcRParamsTree.insertKidTree(Exp());
        while (curSymbolIs("COMMA")) {
            funcRParamsTree.insertKidTree(new VtNode(",", getCurToken()));
            nextSymbol();
            funcRParamsTree.insertKidTree(Exp());
        }
        return funcRParamsTree;
    }

    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    //        → UnaryExp { ('*' | '/' | '%') UnaryExp }
    private GrammarTree MulExp() {
        ArrayList<GrammarTree> mulExpTreeList = new ArrayList<>();
        int i = 0;
        mulExpTreeList.add(new GrammarTree("MulExp", false));
        mulExpTreeList.get(i).insertKidTree(UnaryExp());
        while (curSymbolIs("MULT") || curSymbolIs("DIV") || curSymbolIs("MOD")) {
            mulExpTreeList.add(new GrammarTree("MulExp", false));
            i++;
            mulExpTreeList.get(i).insertKidTree(mulExpTreeList.get(i - 1));
            mulExpTreeList.get(i).insertKidTree(new VtNode(getCurToken().getTokenValue(), getCurToken()));
            nextSymbol();
            mulExpTreeList.get(i).insertKidTree(UnaryExp());
        }
        return mulExpTreeList.get(i);
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    //        → MulExp { ('+' | '−') MulExp }
    private GrammarTree AddExp() {
        ArrayList<GrammarTree> addExpTreeList = new ArrayList<>();
        int i = 0;
        addExpTreeList.add(new GrammarTree("AddExp", false));
        addExpTreeList.get(i).insertKidTree(MulExp());
        while (curSymbolIs("PLUS") || curSymbolIs("MINU")) {
            addExpTreeList.add(new GrammarTree("AddExp", false));
            i++;
            addExpTreeList.get(i).insertKidTree(addExpTreeList.get(i - 1));
            addExpTreeList.get(i).insertKidTree(new VtNode(getCurToken().getTokenValue(), getCurToken()));
            nextSymbol();
            addExpTreeList.get(i).insertKidTree(MulExp());
        }
        return addExpTreeList.get(i);
    }

    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    //        → AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    private GrammarTree RelExp() {
        ArrayList<GrammarTree> relExpTreeList = new ArrayList<>();
        int i = 0;
        relExpTreeList.add(new GrammarTree("RelExp", false));
        relExpTreeList.get(i).insertKidTree(AddExp());
        while (curSymbolIs("LSS") || curSymbolIs("LEQ") || curSymbolIs("GRE") || curSymbolIs("GEQ")) {
            relExpTreeList.add(new GrammarTree("RelExp", false));
            i++;
            relExpTreeList.get(i).insertKidTree(relExpTreeList.get(i - 1));
            relExpTreeList.get(i).insertKidTree(new VtNode(getCurToken().getTokenValue(), getCurToken()));
            nextSymbol();
            relExpTreeList.get(i).insertKidTree(AddExp());
        }
        return relExpTreeList.get(i);
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    //       → RelExp { ('==' | '!=') RelExp }
    private GrammarTree EqExp() {
        ArrayList<GrammarTree> eqExpTreeList = new ArrayList<>();
        int i = 0;
        eqExpTreeList.add(new GrammarTree("EqExp", false));
        eqExpTreeList.get(i).insertKidTree(RelExp());
        while (curSymbolIs("EQL") || curSymbolIs("NEQ")) {
            eqExpTreeList.add(new GrammarTree("EqExp", false));
            i++;
            eqExpTreeList.get(i).insertKidTree(eqExpTreeList.get(i - 1));
            eqExpTreeList.get(i).insertKidTree(new VtNode(getCurToken().getTokenValue(), getCurToken()));
            nextSymbol();
            eqExpTreeList.get(i).insertKidTree(RelExp());
        }
        return eqExpTreeList.get(i);
    }

    // LAndExp → EqExp | LAndExp '&&' EqExp
    //         → EqExp { '&&' EqExp }
    private GrammarTree LAndExp() {
        ArrayList<GrammarTree> lAndExpTreeList = new ArrayList<>();
        int i = 0;
        lAndExpTreeList.add(new GrammarTree("LAndExp", false));
        lAndExpTreeList.get(i).insertKidTree(EqExp());
        while (curSymbolIs("AND")) {
            lAndExpTreeList.add(new GrammarTree("LAndExp", false));
            i++;
            lAndExpTreeList.get(i).insertKidTree(lAndExpTreeList.get(i - 1));
            lAndExpTreeList.get(i).insertKidTree(new VtNode("&&", getCurToken()));
            nextSymbol();
            lAndExpTreeList.get(i).insertKidTree(EqExp());
        }
        return lAndExpTreeList.get(i);
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    //        → LAndExp { '||' LAndExp }
    private GrammarTree LOrExp() {
        ArrayList<GrammarTree> lOrExpTreeList = new ArrayList<>();
        int i = 0;
        lOrExpTreeList.add(new GrammarTree("LOrExp", false));
        lOrExpTreeList.get(i).insertKidTree(LAndExp());
        while (curSymbolIs("OR")) {
            lOrExpTreeList.add(new GrammarTree("LOrExp", false));
            i++;
            lOrExpTreeList.get(i).insertKidTree(lOrExpTreeList.get(i - 1));
            lOrExpTreeList.get(i).insertKidTree(new VtNode("||", getCurToken()));
            nextSymbol();
            lOrExpTreeList.get(i).insertKidTree(LAndExp());
        }
        return lOrExpTreeList.get(i);
    }

    // ConstExp → AddExp
    private GrammarTree ConstExp() {
        GrammarTree constExpTree = new GrammarTree("ConstExp", false);
        constExpTree.insertKidTree(AddExp());
        return constExpTree;
    }
}
