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

        //設定路由和處理程序(handler)
        router.route().handler(BodyHandler.create());
        router.get("/api/v1/word-state").handler(this::wordChangeHandler);
        router.put("/api/v1/server-state").handler(this::setStateHandler);
        router.get("/api/v1/server-state").handler(this::getStateHandler);

        //啟動 HTTP 伺服器
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, serverStart -> {
                    if (serverStart.succeeded()){
                        startPromise.complete();
                        System.out.println("HTTP server start");
                    } else {
                        startPromise.fail(serverStart.cause().getLocalizedMessage());
                    }
                });
    }

    private void wordChangeHandler(RoutingContext context) {
        String input = context.getBodyAsString();
        JsonObject request = new JsonObject()
                .put("action", "wordChangeHandler")
                .put("input", input);

        forwardRequest(request, context);
    }

    private void setStateHandler(RoutingContext context){
        int newState = Integer.parseInt(context.getBodyAsString());
        JsonObject request = new JsonObject()
                .put("action", "SetStateHandler")
                .put("newState", newState);

        forwardRequest(request, context);
    }

    private void getStateHandler(RoutingContext context){
        JsonObject request = new JsonObject()
                .put("action", "GetStateHandler");

        forwardRequest(request, context);
    }

    //處理傳入的 HTTP 請求，並透過 EventBus 將請求轉發給 MessageVerticle
    private void forwardRequest(JsonObject apiRequest, RoutingContext context) {
        EventBus eventBus = vertx.eventBus();

        eventBus.request("apiRequest", apiRequest, reply -> {
                if (reply.succeeded()){
                    context.response()
                            .setStatusCode(200)
                            .end(reply.result().body().toString());

                    System.out.println("Web verticle start");
                } else {
                    context.response()
                            .setStatusCode(500)
                            .end("伺服器連接錯誤");

                    System.out.println(reply.cause().getLocalizedMessage());
                }
        });
    }
}
