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
            case ConstantValue.GNL:
                getNewsList().onComplete(rs -> {
                    if (rs.succeeded()) {
                        message.reply(rs.result());
                    } else {
                        message.fail(404, "無法取得清單：" + rs.cause().getLocalizedMessage());
                    }
                });
                break;
            case ConstantValue.GAN:
                getArticleNum(stationName).onComplete(rs -> codeResponse(message, rs));
                break;
            case ConstantValue.GWN:
                getWordNum(stationName).onComplete(rs -> codeResponse(message, rs));
                break;
            case ConstantValue.GAL:
                getArticleList(stationName).onComplete(rs -> codeResponse(message, rs));
                break;
            case ConstantValue.GTL:
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
    private Future<JsonObject> executeQuery(String sql, JsonObject params, JsonObject item) {
        try {
            JsonArray resultArray = new JsonArray();
            JsonObject result = new JsonObject()
                    .put("code", 200);

            // try-with-resources
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // 檢驗是否有變數需要參數化
                if (!params.isEmpty()) {
                    for (int i=1; i<=params.size(); i++) {
                        pstmt.setObject(i, params.getString(String.valueOf(i)));
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
                            for (int i=1; i<=item.size(); i++) {
                                rsObject.put(item.getString(Integer.toString(i)), rs.getObject(i));
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
        JsonObject nullObject = new JsonObject();
        JsonObject stationObject = new JsonObject()
                .put("1", "stationName");
        return executeQuery(sql, nullObject, stationObject);
    }

    //返回文章總數
    private Future<JsonObject> getArticleNum(String stationName) {
        String sql = SqlQuery.SQL_GET_ARTICLE_NUM(stationName);
        JsonObject nullObject = new JsonObject();
        return executeQuery(sql, nullObject, nullObject);
    }

    //返回所有文章字數總數
    private Future<JsonObject> getWordNum(String stationName) {
        String sql = SqlQuery.SQL_GET_WORD_NUM(stationName);
        JsonObject nullObject = new JsonObject();
        return executeQuery(sql, nullObject, nullObject);
    }

    //返回文章清單
    private Future<JsonObject> getArticleList(String stationName) {
        String sql = SqlQuery.SQL_GET_ARTICLE_LIST(stationName);
        JsonObject nullObject = new JsonObject();
        JsonObject articleObject = new JsonObject()
                .put("1", "id")
                .put("2", "title")
                .put("3", "url")
                .put("4", "time");
        return executeQuery(sql, nullObject, articleObject);
    }

    //返回標題清單
    private Future<JsonObject> getTitleList(String stationName, String keyword) {
        String sql = SqlQuery.SQL_GET_TITLE_LIST(stationName);
        JsonObject paramsObject = new JsonObject()
                .put("1", "%" + keyword + "%");
        JsonObject titleObject = new JsonObject()
                .put("1", "id")
                .put("2", "title")
                .put("3", "url");
        return executeQuery(sql, paramsObject, titleObject);
    }

    public void stop() {
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}
