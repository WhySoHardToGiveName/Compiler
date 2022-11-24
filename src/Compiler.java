import ir.Module;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws IOException
    {
        String inputFileName = "testfile.txt";
       /* FileReader fr = new FileReader(inputFileName);
        BufferedReader br = new BufferedReader(fr);
        char[] buffer = new char[1024];
        br.read(buffer);
        System.err.println(buffer);*/
        lexical_analyzer.comment_filter(inputFileName);
        ArrayList<Token> tokenList = lexical_analyzer.token_getter(false, "output.txt");
        error_handler errorHandler = new error_handler();
        syntactic_analyzer parser = new syntactic_analyzer(tokenList, errorHandler);
        GrammarTree grammarTree = parser.CompUnit();
        //grammarTree.printTree("output.txt");
        SymbolTableCreator symbolTableCreator = new SymbolTableCreator(errorHandler);
        SymbolTable symbolTable = symbolTableCreator.CompUnit(grammarTree);
        //errorHandler.printErrorList("error.txt");
        Module.getModule().print("llvm_ir.txt");
    }
}
