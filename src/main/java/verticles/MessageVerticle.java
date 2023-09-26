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

        if("changeWord".equals(action)) {
            String input = request.getString("input");
            JFormat(input);
        } else if ("setState".equals(action)) {
            int newState = request.getInteger("newState");
            SetState(newState);
        } else if ("getState".equals(action)) {
            GetState();
        } else {
            message.fail(400, "錯誤操作");
        }
    }

    //返回一個JSON物件
    private Future<JsonObject> JFormat(String input) {
        try {
            //計算字數
            int length = input.length();
            //將字串轉為大寫
            String upper = input.toUpperCase();
            //json格式
            JsonObject jString = new JsonObject()
                    .put("length", length)
                    .put("upper", upper);

            //操作成功，返回jString
            return Future.succeededFuture(jString);
        } catch (DecodeException e) {
            //操作失敗，返回異常原因
            return Future.failedFuture(e.getMessage());
        }
    }

    //更新狀態
    private Future<Void> SetState(int newState) {
        if (newState <= 3 && newState >= 0) {
            serverState = newState;
            return Future.succeededFuture();
        } else {
            return Future.failedFuture("狀態值無效，狀態值範圍為0~3");
        }
    }

    //查詢狀態
    private Future<Integer> GetState() {
        return Future.succeededFuture(serverState);
    }
}
