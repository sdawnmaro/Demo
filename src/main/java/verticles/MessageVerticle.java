package verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

public class MessageVerticle extends AbstractVerticle {
    //maintain state 初始值預設為 0
    private int serverState = 0;

    public void start(){
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer("apiRequest", this::handleApiRequest);
    }

    private void handleApiRequest(Message<JsonObject> message) {
        //檢測有透過eventBus串接成功
        System.out.println("處理 HTTP 需求");

        JsonObject request = message.body();
        String action = request.getString("action");

        switch (action) {
            case "wordChangeHandler":
                String input = request.getString("input");
                wordChangeHandler(input).onComplete(rs -> {
                    if (rs.succeeded()) {
                        message.reply(rs.result());
                    } else {
                        message.fail(400, "API 執行失敗：" + rs.cause().getLocalizedMessage());
                    }
                });
                break;
            case "setStateHandler":
                int newState = request.getInteger("newState");
                setStateHandler(newState).onComplete(rs -> {
                   if (rs.succeeded()) {
                       message.reply(rs.result());
                   } else {
                       message.fail(400, "API 執行失敗：" + rs.cause().getLocalizedMessage());
                   }
                });
                break;
            case "getStateHandler":
                getStateHandler().onComplete(rs -> {
                    if (rs.succeeded()) {
                        message.reply(rs.result());
                    } else {
                        message.fail(400, "API 執行失敗：" + rs.cause().getLocalizedMessage());
                    }
                });
                break;
            default:
                message.fail(400, "錯誤操作");
                break;
        }
    }

    //返回一個JSON物件
    private Future<JsonObject> wordChangeHandler(String input) {
        try {
            if (input != null) {
                //計算字數
                int length = input.length();
                //將字串轉為大寫
                String upper = input.toUpperCase();
                //json格式
                JsonObject responseWord = new JsonObject()
                        .put("length", length)
                        .put("upper", upper);

                System.out.println("成功計算字串，並轉為大寫");

                //操作成功，返回jString
                return Future.succeededFuture(responseWord);
            } else {
                return Future.failedFuture("輸入字串為空，無法對字串進行計算與轉換");
            }
        } catch (DecodeException e) {
            //操作失敗，返回異常原因
            return Future.failedFuture(e.getLocalizedMessage());
        }
    }

    //更新狀態
    private Future<JsonObject> setStateHandler(int newState) {
        try {
            if (newState <= 3 && newState >= 0) {
                //設定新狀態
                serverState = newState;
                JsonObject responseState = new JsonObject()
                        .put("state", newState);

                System.out.println("成功更新Server狀態");
                return Future.succeededFuture(responseState);
            } else {
                return Future.failedFuture("狀態值無效，狀態值範圍為0~3");
            }
        } catch (DecodeException e) {
            return Future.failedFuture(e.getLocalizedMessage());
        }
    }

    //查詢狀態
    private Future<JsonObject> getStateHandler() {
        System.out.println("成功獲取Server狀態");
        JsonObject responseState = new JsonObject()
                .put("state", serverState);
        return Future.succeededFuture(responseState);
    }
}
