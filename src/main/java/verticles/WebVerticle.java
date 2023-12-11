package verticles;

import constant.ConstantValue;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class WebVerticle extends AbstractVerticle {
    public void start(Promise<Void> startPromise){
        // 建立 Router 來處理 HTTP 的請求
        Router router = Router.router(vertx);

        // 設定路由和處理程序(handler)
        router.route().handler(BodyHandler.create());
        router.get("/api/v1/news/info").handler(this::newsListHandler);
        router.get("/api/v1/news/:name/article-num").handler(this::articleNumHandler);
        router.get("/api/v1/news/:name/word-num").handler(this::wordNumHandler);
        router.get("/api/v1/news/:name/article-list").handler(this::articleListHandler);
        router.get("/api/v1/news/:name/title-list").handler(this::titleListHandler);

        // 設定 HTTP 伺服器選項(SSL配置)
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setSsl(true)
                .setPemKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath("/Users/mashimaro511311/IdeaProjects/PracticeDemo/KAC/selfsigned.crt")
                        .setKeyPath("/Users/mashimaro511311/IdeaProjects/PracticeDemo/KAC/privatekey.pem"));

        // 啟動 HTTP 伺服器
        vertx.createHttpServer(httpServerOptions)
                .requestHandler(router)
                .listen(8443, serverStart -> {
                    if (serverStart.succeeded()){
                        startPromise.complete();
                        System.out.println("HTTP server start");
                    } else {
                        startPromise.fail(serverStart.cause().getLocalizedMessage());
                    }
                });
    }

    // 捕獲錯誤回應
    private void catchError(RoutingContext context, Exception e){
        if (e instanceof IllegalArgumentException) {
            System.out.println("無效的參數：" + e.getLocalizedMessage());
            context.response()
                    .setStatusCode(400)
                    .end("無效的參數：" + e.getLocalizedMessage());
        } else {
            System.out.println(e.getLocalizedMessage());
            context.response()
                    .setStatusCode(500)
                    .end("伺服器錯誤：" + e.getLocalizedMessage());
        }
    }

    //新聞站台清單
    private void newsListHandler(RoutingContext context) {
            JsonObject request = new JsonObject()
                    .put("action", ConstantValue.actionGetNewsList);

            forwardRequest(request, context);
    }

    //文章總數
    private void articleNumHandler(RoutingContext context) {
        try {
            // 獲取 :name 參數值
            String stationName = context.pathParam("name");

            JsonObject request = new JsonObject()
                    .put("action", ConstantValue.actionGetArticleNum)
                    .put("name", stationName);

            forwardRequest(request, context);
        } catch (Exception e) {
            catchError(context, e);
        }
    }

    //所有文章字數總數
    private void wordNumHandler(RoutingContext context) {
        try {
            // 獲取 :name 參數值
            String stationName = context.pathParam("name");

            JsonObject request = new JsonObject()
                    .put("action", ConstantValue.actionGetWordNum)
                    .put("name", stationName);

            forwardRequest(request, context);
        } catch (Exception e) {
            catchError(context, e);
        }
    }

    //文章清單
    private void articleListHandler(RoutingContext context) {
        try {
            // 獲取 :name 參數值
            String stationName = context.pathParam("name");

            // 預設頁數為第一頁
            int page = 1;
            // 取得輸入值，從 URL 參數中獲取:https://localhost:8443/api/v1/news/:name/article-list?page=輸入值
            String pageParam = context.request().getParam("page");

            // 使用者未輸入參數值：page=1(預設值)
            // 使用者有輸入有效參數值：將 page 設定為使用者輸入的值
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                try {
                    page = Integer.parseInt(pageParam);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException();
                }
            }

            JsonObject request = new JsonObject()
                    .put("action", ConstantValue.actionGetArticleList)
                    .put("name", stationName)
                    .put("page", page);

            forwardRequest(request, context);
        } catch (Exception e) {
            catchError(context, e);
        }
    }

    //標題清單
    private void titleListHandler(RoutingContext context) {
        try {
            // 獲取 :name 參數值
            String stationName = context.pathParam("name");

            // 取得輸入值，從 URL 參數中獲取:https://localhost:8443/api/v1/news/:name/title-list?keyword=輸入值
            String keyword = context.request().getParam("keyword");

            if (keyword == null || keyword.trim().isEmpty()) {
                throw new IllegalArgumentException();
            }

            JsonObject request = new JsonObject()
                    .put("action", ConstantValue.actionGetTitleList)
                    .put("name", stationName)
                    .put("keyword", keyword);

            forwardRequest(request, context);
        } catch (Exception e) {
            catchError(context, e);
        }
    }

    // 處理傳入的 HTTP 請求，並透過 EventBus 將請求轉發給 MessageVerticle
    private void forwardRequest(JsonObject apiRequest, RoutingContext context) {
        EventBus eventBus = vertx.eventBus();

        eventBus.request("apiCon", apiRequest, reply -> {
                if (reply.succeeded()){
                    context.response()
                            .setStatusCode(200)
                            .end(reply.result().body().toString());

                    System.out.println("請求成功");
                } else {
                    Throwable cause = reply.cause();
                    if (cause instanceof ReplyException){
                        ReplyException replyException = (ReplyException) cause;
                        int errorCode = replyException.failureCode();

                        context.response()
                                .setStatusCode(errorCode)
                                .end(reply.cause().getLocalizedMessage());
                    } else {
                        context.response()
                                .setStatusCode(500)
                                .end(reply.cause().getLocalizedMessage());
                    }

                    System.out.println("請求失敗：" + cause.getLocalizedMessage());
                }
        });
    }
}
