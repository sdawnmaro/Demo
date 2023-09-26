package verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class WebVerticle extends AbstractVerticle {
    public void start(Promise<Void> startPromise){
        //建立 Router 來處理 HTTP 的請求
        Router router = Router.router(vertx);

        //設定路由和處理程序
        router.route().handler(BodyHandler.create());
        router.get("/api/word/").handler(this::wordRequest);
        router.put("/api/state/").handler(this::setState);
        router.get("/api/state/").handler(this::getState);

        //啟動 HTTP 伺服器
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, serverStart -> {
                    if (serverStart.succeeded()){
                        startPromise.complete();
                    } else {
                        startPromise.fail(serverStart.cause());
                    }
                });
    }

    private void wordRequest(RoutingContext context) {
        String input = context.getBodyAsString();
        JsonObject request = new JsonObject()
                .put("action", "changeWord")
                .put("input", input);

        forwardRequest(request, context);
    }

    private void setState(RoutingContext context){
        int newState = Integer.parseInt(context.getBodyAsString());
        JsonObject request = new JsonObject()
                .put("action", "setState")
                .put("newState", newState);

        forwardRequest(request, context);
    }

    private void getState(RoutingContext context){
        JsonObject request = new JsonObject()
                .put("action", "getState");

        forwardRequest(request, context);
    }
    //處理傳入的 HTTP 請求，並透過 EventBus 將請求轉發給 MessageVerticle
    private void forwardRequest(JsonObject request, RoutingContext context) {
        EventBus eventBus = vertx.eventBus();

        eventBus.request("apiRequest", request, reply -> {
                if (reply.succeeded()){
                    context.response()
                            .setStatusCode(200)
                            .end(reply.result().body().toString());
                } else {
                    context.response()
                            .setStatusCode(500)
                            .end("伺服器連接錯誤");
                }
        });
    }
}
