import java.nio.file.Path;
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        try {
            if(args.length!=1 && !(args.length==3&&args[1].equals("--batch")))
                throw new IllegalArgumentException("Usage: Main input.json [--batch B1]");
            Dataset data=new Dataset(Json.read(Path.of(args[0])));
            System.out.println(Json.write(new ProtocolService(data).makeProtocol(args.length==3?args[2]:"B1")));
        } catch(Exception e) { System.err.println("Error: "+e.getMessage()); System.exit(2); }
    }
}
