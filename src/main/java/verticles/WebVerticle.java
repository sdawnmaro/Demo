package verticles;

import constant.ConstantValue;
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
        router.get("/api/v1/word-state").handler(this::getWordState);
        router.put("/api/v1/server-state").handler(this::updateServerState);
        router.get("/api/v1/server-state").handler(this::getServerState);

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

    private void getWordState(RoutingContext context) {
        try {
            //從 URL 參數中獲取資料:/api/v1/word-state?input=輸入值
            String input = context.request().getParam("input");

            //若 input 為空，拋出例外
            if (input == null) {
                throw new IllegalArgumentException();
            }

            JsonObject request = new JsonObject()
                    .put("action", ConstantValue.GWS)
                    .put("input", input);

            forwardRequest(request, context);
        } catch (IllegalArgumentException e) {
            System.out.println("參數值為空：" + e.getLocalizedMessage());
        }
    }

    private void updateServerState(RoutingContext context){
        try {
            //從 Request Body 獲取資料
            JsonObject stateValue = context.getBodyAsJson();

            //若 stateValue 為空，拋出例外
            if (!stateValue.containsKey("newState")) {
                throw new IllegalArgumentException();
            }

            int newState = stateValue.getInteger("newState");

            JsonObject request = new JsonObject()
                    .put("action", ConstantValue.USS)
                    .put("newState", newState);

            forwardRequest(request, context);
        } catch (IllegalArgumentException e) {
            System.out.println("參數值為空：" + e.getLocalizedMessage());
        }
    }

    private void getServerState(RoutingContext context){
        JsonObject request = new JsonObject()
                .put("action", ConstantValue.GSS);

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
