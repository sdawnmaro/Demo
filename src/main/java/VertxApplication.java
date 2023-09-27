import verticles.MessageVerticle;
import io.vertx.core.Vertx;
import verticles.WebVerticle;

public class VertxApplication {
    private static Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        //部署web api verticle
        vertx.deployVerticle(new WebVerticle(), result ->{
            if (result.succeeded()) {
                System.out.println("WebVerticle 部署成功");
            } else {
                System.out.println("WebVerticle 部署失敗：" + result.cause().getLocalizedMessage());
            }
        });
        //部署message process verticle
        vertx.deployVerticle(new MessageVerticle(), result ->{
            if (result.succeeded()) {
                System.out.println("MessageVerticle 部署成功");
            } else {
                System.out.println("MessageVerticle 部署失敗：" + result.cause().getLocalizedMessage());
            }
        });
    }
}
