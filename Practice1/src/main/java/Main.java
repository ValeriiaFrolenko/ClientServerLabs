import core.ServerApplication;

public class Main {
    public static void main(String[] args) {
        ServerApplication serverApplication = new ServerApplication();
        Runtime.getRuntime().addShutdownHook(new Thread(serverApplication::stop));
        serverApplication.start();
    }
}
