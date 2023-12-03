package verticles;

import constant.ConstantValue;
import constant.SqlQuery;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.sql.*;

public class MessageVerticle extends AbstractVerticle {
    // 資料庫資訊
    private static final String url = "jdbc:mysql://news-crawler.iot.iptnet.net:53306/finance";
    private static final String username = "bronci";
    private static final String password = "Bronci@25559100";
    private Connection conn;

    public void start(){
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer("apiCon", this::handleApiRequest);

        // 連接資料庫
        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            System.out.println("無法連接至資料庫：" + e.getLocalizedMessage());
        }
    }

    private void handleApiRequest(Message<JsonObject> message) {
        //檢測有透過eventBus串接成功
        System.out.println("處理 HTTP 需求");

        JsonObject request = message.body();
        String action = request.getString("action");
        String stationName = request.getString("name");

        switch (action) {
            case ConstantValue.actionGetNewsList:
                getNewsList().onComplete(rs -> {
                    if (rs.succeeded()) {
                        message.reply(rs.result());
                    } else {
                        message.fail(404, "無法取得清單：" + rs.cause().getLocalizedMessage());
                    }
                });
                break;
            case ConstantValue.actionGetArticleNum:
                getArticleNum(stationName).onComplete(rs -> codeResponse(message, rs));
                break;
            case ConstantValue.actionGetWordNum:
                getWordNum(stationName).onComplete(rs -> codeResponse(message, rs));
                break;
            case ConstantValue.actionGetArticleList:
                getArticleList(stationName).onComplete(rs -> codeResponse(message, rs));
                break;
            case ConstantValue.actionGetTitleList:
                String keyword = request.getString("keyword");
                getTitleList(stationName, keyword).onComplete(rs -> codeResponse(message, rs));
                break;
            default:
                message.fail(500, "Internal Server Error");
                break;
        }
    }

    //代碼回應
    private void codeResponse(Message<JsonObject> message, AsyncResult<JsonObject> rs){
        if (rs.succeeded()) {
            message.reply(rs.result());
        } else {
            String errorCode = rs.cause().getMessage();
            if ("400".equals(errorCode)) {
                message.fail(400, "Bad Request：" + rs.cause().getLocalizedMessage());
            } else if ("404".equals(errorCode)) {
                message.fail(404, "Not Found：" + rs.cause().getLocalizedMessage());
            } else {
                message.fail(500, "Internal Server Error：" + rs.cause().getLocalizedMessage());
            }
            System.out.println("錯誤回應");
        }
    }

    // code reuse: 查詢資料庫
    private Future<JsonObject> executeQuery(String sql, JsonArray params, JsonArray item) {
        try {
            JsonArray resultArray = new JsonArray();
            JsonObject result = new JsonObject().put("code", 200);

            // try-with-resources
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // 檢驗是否有變數需要參數化
                if (!params.isEmpty()) {
                    // JsonArray 索引值從0開始
                    for (int i=0; i<params.size(); i++) {
                        pstmt.setObject(i+1, params.getValue(i));
                    }
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    // 檢驗"data"的內容是否有需要jsonObject格式
                    if (item.isEmpty()) {
                        if (rs.next()) {
                            Object num = rs.getObject(1);
                            result.put("data", num);
                        }
                    } else {
                        while (rs.next()) {
                            JsonObject rsObject = new JsonObject();
                            for (int i=0; i<item.size(); i++) {
                                rsObject.put(item.getString(i), rs.getObject(i+1));
                            }

                            resultArray.add(rsObject);
                        }
                        result.put("data", resultArray);
                    }
                }
            }

            return Future.succeededFuture(result);
        } catch (SQLException e) {
            return Future.failedFuture(e.getLocalizedMessage());
        }
    }

    // 返回新聞站台清單
    private Future<JsonObject> getNewsList() {
        String sql = SqlQuery.SQL_GET_NEWS_LIST;
        JsonArray nullParams = new JsonArray();
        JsonArray stationArray = new JsonArray().add("stationName");
        JsonObject stationObject = new JsonObject()
                .put("1", "stationName");
        return executeQuery(sql, nullParams, stationArray);
    }

    //返回文章總數
    private Future<JsonObject> getArticleNum(String stationName) {
        String sql = SqlQuery.SQL_GET_ARTICLE_NUM(stationName);
        JsonArray nullParams = new JsonArray();
        return executeQuery(sql, nullParams, nullParams);
    }

    //返回所有文章字數總數
    private Future<JsonObject> getWordNum(String stationName) {
        String sql = SqlQuery.SQL_GET_WORD_NUM(stationName);
        JsonArray nullParams = new JsonArray();
        return executeQuery(sql, nullParams, nullParams);
    }

    //返回文章清單
    private Future<JsonObject> getArticleList(String stationName) {
        String sql = SqlQuery.SQL_GET_ARTICLE_LIST(stationName);
        JsonArray nullParams = new JsonArray();
        JsonArray articleArray = new JsonArray()
                .add("id")
                .add("title")
                .add("url")
                .add("time");
        JsonObject articleObject = new JsonObject()
                .put("1", "id")
                .put("2", "title")
                .put("3", "url")
                .put("4", "time");
        return executeQuery(sql, nullParams, articleArray);
    }

    //返回標題清單
    private Future<JsonObject> getTitleList(String stationName, String keyword) {
        String sql = SqlQuery.SQL_GET_TITLE_LIST(stationName);
        JsonArray paramsArray = new JsonArray().add("%" + keyword + "%");
        JsonArray titleArray = new JsonArray()
                .add("id")
                .add("title")
                .add("url");
        JsonObject titleObject = new JsonObject()
                .put("1", "id")
                .put("2", "title")
                .put("3", "url");
        return executeQuery(sql, paramsArray, titleArray);
    }

    public void stop() {
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}
