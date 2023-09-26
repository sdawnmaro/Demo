import verticles.MessageVerticle;
import io.vertx.core.Vertx;
import verticles.WebVerticle;

public class VertxApplication {
    private static Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        //部署web api verticle
        vertx.deployVerticle(new WebVerticle());
        //部署message process verticle
        vertx.deployVerticle(new MessageVerticle());
    }
}
