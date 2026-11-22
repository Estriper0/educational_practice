import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;
/** Small standalone JSON codec; accepts standard JSON numbers only. */
public final class Json {
    private final String source;
    private int offset;
    private static final Pattern NUMBER=Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
    private Json(String source) { this.source=source; }
    public static Object read(Path path) throws IOException {
        Json p=new Json(Files.readString(path)); Object v=p.value(); p.space();
        if(p.offset!=p.source.length()) throw p.error(); return v;
    }
    private IllegalArgumentException error() { return new IllegalArgumentException("Invalid JSON at "+offset); }
    private void space() { while(offset<source.length() && " \r\n\t".indexOf(source.charAt(offset))>=0) offset++; }
    private boolean eat(char c) { space(); if(offset<source.length()&&source.charAt(offset)==c) {offset++; return true;} return false; }
    private Object value() {
        space(); if(offset>=source.length()) throw error(); char c=source.charAt(offset);
        if(c=='"') return string();
        if(eat('{')) {
            Map<String,Object> m=new LinkedHashMap<>(); if(eat('}')) return m;
            do { String key=string(); if(!eat(':')) throw error(); m.put(key,value()); } while(eat(','));
            if(!eat('}')) throw error(); return m;
        }
        if(eat('[')) {
            List<Object> a=new ArrayList<>(); if(eat(']')) return a;
            do {a.add(value());} while(eat(',')); if(!eat(']')) throw error(); return a;
        }
        for(String literal:List.of("true","false","null")) if(source.startsWith(literal,offset)) {
            offset+=literal.length(); return literal.equals("null") ? null : literal.equals("true");
        }
        Matcher m=NUMBER.matcher(source); m.region(offset,source.length()); if(!m.lookingAt()) throw error();
        offset=m.end(); return new java.math.BigDecimal(m.group());
    }
    private String string() {
        if(!eat('"')) throw error(); StringBuilder b=new StringBuilder();
        while(offset<source.length()) {
            char c=source.charAt(offset++); if(c=='"') return b.toString();
            if(c=='\\') {
                if(offset==source.length()) throw error(); c=source.charAt(offset++);
                switch(c) {
                    case '"','\\','/' -> b.append(c);
                    case 'b' -> b.append('\b'); case 'f' -> b.append('\f');
                    case 'n' -> b.append('\n'); case 'r' -> b.append('\r'); case 't' -> b.append('\t');
                    case 'u' -> { if(offset+4>source.length()) throw error();
                        try {b.append((char)Integer.parseInt(source.substring(offset,offset+4),16));} catch(NumberFormatException e) {throw error();} offset+=4; }
                    default -> throw error();
                }
            } else {if(c<32) throw error(); b.append(c);}
        }
        throw error();
    }
    public static String write(Object v) {
        if(v==null) return "null";
        if(v instanceof String s) {
            StringBuilder b=new StringBuilder("\"");
            for(char c:s.toCharArray()) switch(c) {
                case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n"); case '\r' -> b.append("\\r"); case '\t' -> b.append("\\t");
                default -> {if(c<32) b.append(String.format("\\u%04x",(int)c)); else b.append(c);}
            }
            return b.append('"').toString();
        }
        if(v instanceof Map<?,?> m) {List<String> parts=new ArrayList<>(); m.forEach((k,x)->parts.add(write(k.toString())+":"+write(x))); return "{"+String.join(",",parts)+"}";}
        if(v instanceof Collection<?> a) return "["+String.join(",",a.stream().map(Json::write).toList())+"]";
        if(v instanceof Number || v instanceof Boolean) return v.toString();
        throw new IllegalArgumentException("Unsupported JSON value");
    }
}
