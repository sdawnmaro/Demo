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
        JsonObject request = message.body();
        String action = request.getString("action");

        switch (action) {
            case "wordChangeHandler":
                String input = request.getString("input");
                wordChangeHandler(input);
                message.reply("成功呼叫 wordChange API");
                break;
            case "SetStateHandler":
                int newState = request.getInteger("newState");
                SetStateHandler(newState);
                message.reply("成功呼叫 setState API");
                break;
            case "GetStateHandler":
                GetStateHandler();
                message.reply("成功呼叫 getState API");
                break;
            default:
                message.fail(400, "錯誤操作");
                break;
        }
    }

    //返回一個JSON物件
    private Future<JsonObject> wordChangeHandler(String input) {
        try {
            //計算字數
            int length = input.length();
            //將字串轉為大寫
            String upper = input.toUpperCase();
            //json格式
            JsonObject jString = new JsonObject()
                    .put("length", length)
                    .put("upper", upper);
            System.out.println("成功獲取字串資訊");

            //操作成功，返回jString
            return Future.succeededFuture(jString);
        } catch (DecodeException e) {
            //操作失敗，返回異常原因
            return Future.failedFuture(e.getMessage());
        }
    }

    //更新狀態
    private Future<Void> SetStateHandler(int newState) {
        if (newState <= 3 && newState >= 0) {
            serverState = newState;
            System.out.println("成功更新Server狀態");
            return Future.succeededFuture();
        } else {
            return Future.failedFuture("狀態值無效，狀態值範圍為0~3");
        }
    }

    //查詢狀態
    private Future<Integer> GetStateHandler() {
        System.out.println("成功獲取Server狀態");
        return Future.succeededFuture(serverState);
    }
}
