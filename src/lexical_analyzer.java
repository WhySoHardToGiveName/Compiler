import java.io.*;
import java.util.ArrayList;

public class lexical_analyzer {
    public static void comment_filter(String inputFileName) throws IOException {
        FileReader fr = new FileReader(inputFileName);
        BufferedReader bufferedreader = new BufferedReader(fr);
        FileWriter fw = new FileWriter(new File("notes_filter_out.txt"));
        BufferedWriter bw = new BufferedWriter(fw);
        char ch, tmp;
        boolean inMultiComments = false, inSingleComments = false, inString = false;
        try{
            while((ch = (char)bufferedreader.read()) != (char)-1) {
                if(ch == '\r')
                    continue;
                if(inMultiComments){       //多行注释
                    if(ch == '\n') {
                        bw.write(ch);
                    } else if (ch == '*') {
                        bufferedreader.mark(1);
                        if((char)bufferedreader.read() == '/') {
                            inMultiComments = false;
                        } else{
                            bufferedreader.reset();
                        }
                    }
                } else{                 //非多行注释
                    if(inSingleComments){  //单行注释
                        if(ch == '\n') {
                            bw.write(ch);
                            inSingleComments = false;
                        }
                    } else{             //非单行注释
                        if(ch == '/') {
                            bufferedreader.mark(1);
                            if((tmp = (char)bufferedreader.read()) == '/') {
                                if (!inString){
                                    inSingleComments = true;
                                    continue;
                                }
                            } else if(tmp == '*') {
                                if (!inString) {
                                    inMultiComments = true;
                                    continue;
                                }
                            }
                            bufferedreader.reset();
                        } else if(ch == '"') {
                            inString = !inString;
                        }
                        bw.write(ch);
                    }
                }
            }
            bufferedreader.close();
            bw.close();
            fr.close();
            fw.close();
            System.out.println("comment filtered");
        }
        catch (Exception ioe){
            ioe.printStackTrace();
        }
    }
    public static ArrayList<Token> token_getter(boolean toOutput, String outputPath) throws IOException{
        FileReader fr = new FileReader("notes_filter_out.txt");
        BufferedReader br = new BufferedReader(fr);
        ArrayList<Token> tokenList = new ArrayList<>();
        int row = 1;
        StringBuilder curToken = new StringBuilder();
        char ch;
        boolean inIdent = false, inIntConst = false, inStringConst = false, inOperator = false, inInitial = true;
        try{
            while((ch = (char)br.read()) != (char)-1) {
                if (ch == '\n') {
                    row++;
                    inInitial = true;
                    continue;
                }
                if (inInitial) {
                    TokenMap.TYPE type = TokenMap.getType(ch);
                    switch (type){
                        case LETTER:    //标识符状态
                            inIdent = true;
                            inInitial = false;
                            curToken.append(ch);
                            br.mark(1);
                            while((ch = (char)br.read()) != (char)-1){
                                if(TokenMap.getType(ch) == TokenMap.TYPE.LETTER || TokenMap.getType(ch) == TokenMap.TYPE.DIGIT){
                                    curToken.append(ch);
                                } else{
                                    br.reset();
                                    inIdent = false;
                                    inInitial = true;
                                    break;
                                }
                                br.mark(1);
                            }
                            if(TokenMap.isKeyword(curToken.toString())){
                                String tokenCode = TokenMap.getTokenCode(curToken.toString());
                                tokenList.add(new Token(tokenCode, curToken.toString(), row));
                            } else{
                                tokenList.add(new Token("IDENFR", curToken.toString(), row));
                            }
                            break;
                        case DIGIT:     //数字串状态
                            inIntConst = true;
                            inInitial = false;
                            if(ch == '0'){
                                br.mark(1);
                                char tmp = (char)br.read();
                                if(TokenMap.getType(tmp) == TokenMap.TYPE.DIGIT){
                                    System.err.println("Error: Line " + row + ": 0 cannot be the first digit of a number.");
                                }
                                br.reset();
                            }
                            curToken.append(ch);
                            br.mark(1);
                            while((ch = (char)br.read()) != (char)-1){
                                if(TokenMap.getType(ch) == TokenMap.TYPE.DIGIT){
                                    curToken.append(ch);
                                } else{
                                    br.reset();
                                    inIntConst = false;
                                    inInitial = true;
                                    break;
                                }
                                br.mark(1);
                            }
                            tokenList.add(new Token("INTCON", curToken.toString(), row));
                            break;
                        case QUOTE:     //字符串状态
                            inStringConst = true;
                            inInitial = false;
                            curToken.append(ch);
                            while((ch = (char)br.read()) != (char)-1){
                                if(ch == '\n'){
                                    System.err.println("Error: Line " + row + ":String not end!");
                                    inStringConst = false;
                                    inInitial = true;
                                    break;
                                } else{
                                    curToken.append(ch);
                                    if(ch == '"'){
                                        inStringConst = false;
                                        inInitial = true;
                                        break;
                                    }
                                }
                            }
                            tokenList.add(new Token("STRCON", curToken.toString(), row));
                            break;
                        case OPERATOR:  //运算符状态
                            inOperator = true;
                            inInitial = false;
                            if(ch == '&' || ch == '|'){
                                curToken.append(ch);
                                br.mark(1);
                                if((char)br.read() == ch){
                                    curToken.append(ch);
                                } else{
                                    br.reset();
                                }
                            } else if(ch == '<' || ch == '>' || ch == '=' || ch == '!'){
                                curToken.append(ch);
                                br.mark(1);
                                if((char)br.read() == '='){
                                    curToken.append('=');
                                } else{
                                    br.reset();
                                }
                            } else{
                                curToken.append(ch);
                            }
                            inOperator = false;
                            inInitial = true;
                            String tokenCode = TokenMap.getTokenCode(curToken.toString());
                            tokenList.add(new Token(tokenCode, curToken.toString(), row));
                            break;
                        case DELIMITER: //空格界符状态
                            break;
                        case OTHER:     //其他字符状态
                            System.err.println("Error: Line " + row + ":" + ch + " Invalid character in type of OTHER!");
                        default:
                            System.err.println("Error: Line " + row + ":" + ch + " Invalid character out of type range!");
                    }
                    curToken.delete(0, curToken.length());
                } else {
                    if(inIdent)
                        System.err.println("Error: Line " + row + ":still in Ident state!");
                    if(inIntConst)
                        System.err.println("Error: Line " + row + ":still in IntConst state!");
                    if(inStringConst)
                        System.err.println("Error: Line " + row + ":still in StringConst state!");
                    if(inOperator)
                        System.err.println("Error: Line " + row + ":still in Operator state!");
                }
            }
            br.close();
            fr.close();
        } catch (Exception ioe){
            ioe.printStackTrace();
        }
        if(toOutput){
            FileWriter fw = new FileWriter(outputPath);
            BufferedWriter bw = new BufferedWriter(fw);
            for(Token token : tokenList){
                bw.write(token.getTokenCode() + ' ' + token.getTokenValue());
                bw.newLine();
            }
            bw.close();
            fw.close();
        }
        System.out.println("token get\nlexical analysis done!");
        return tokenList;
    }
}
