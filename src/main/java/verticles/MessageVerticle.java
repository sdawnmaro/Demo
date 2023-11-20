package verticles;

import constant.ConstantValue;
import io.vertx.core.AbstractVerticle;
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
            case ConstantValue.AN:
                articleNum(stationName).onComplete(rs -> {
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
                });
                break;
            case ConstantValue.WN:
                wordNum(stationName).onComplete(rs -> {
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
                });
            case ConstantValue.AL:
                articleList(stationName).onComplete(rs -> {
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
                });
            case ConstantValue.TL:
                String keyword = request.getString("keyword");

                titleList(stationName, keyword).onComplete(rs -> {
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
                });
            default:
                message.fail(500, "Internal Server Error");
                break;
        }
    }

    // 返回新聞站台清單
    private Future<JsonObject> getNewsList() {
        try {
            String sql = "SHOW tables";
            JsonArray articleList = new JsonArray();

            // try-with-resources
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    articleList.add(rs.getString(1));
                }
            }

            JsonObject result = new JsonObject()
                    .put("code", 200)
                    .put("data", articleList);

            return Future.succeededFuture(result);
        } catch (SQLException e) {
            return Future.failedFuture(e.getLocalizedMessage());
        }
    }

    //返回文章總數
    private Future<JsonObject> articleNum(String stationName) {
        try {
            int num = 0;
            String sql = "SELECT COUNT(*) FROM " + stationName;

            // try-with-resources
            try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    num = rs.getInt(1);
                }
            }

            JsonObject result = new JsonObject()
                    .put("code", 200)
                    .put("data", num);

            return Future.succeededFuture(result);
        } catch (SQLException e) {
            //return Future.failedFuture(String.valueOf(500));
            return Future.failedFuture(String.valueOf(e.getErrorCode()));
        }
    }

    //返回所有文章字數總數
    private Future<JsonObject> wordNum(String stationName) {
        try {
            int num = 0;
            String sql = "SELECT SUM(CHAR_LENGTH(text)) AS num FROM " + stationName;

            // try-with-resources
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    num = rs.getInt(1);
                }
            }

            JsonObject result = new JsonObject()
                    .put("code", 200)
                    .put("data", num);

            return Future.succeededFuture(result);
        } catch (SQLException e) {
            return Future.failedFuture(String.valueOf(e.getErrorCode()));
        }
    }

    //返回文章清單
    private Future<JsonObject> articleList(String stationName) {
            try {
                String sql = "SELECT id, title, url, publish_date FROM " + stationName;
                JsonArray articleList = new JsonArray();

                // try-with-resources
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        JsonObject article = new JsonObject()
                                .put("id", rs.getInt("id"))
                                .put("title", rs.getString("title"))
                                .put("url", rs.getString("url"))
                                .put("time", rs.getString("publish_date"));

                        articleList.add(article);
                    }
                }

                JsonObject result = new JsonObject()
                        .put("code", 200)
                        .put("data", articleList);

                return Future.succeededFuture(result);
            } catch (SQLException e) {
                return Future.failedFuture(String.valueOf(e.getErrorCode()));
            }
    }

    //返回標題清單
    private Future<JsonObject> titleList(String stationName, String keyword) {
        try {
            String sql = "SELECT id, title, url FROM " + stationName + " WHERE title LIKE ?";
            JsonArray titleList = new JsonArray();

            // try-with-resources
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + keyword + "%");

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        JsonObject title = new JsonObject()
                                .put("id", rs.getInt("id"))
                                .put("title", rs.getString("title"))
                                .put("url", rs.getString("url"));

                        titleList.add(title);
                    }
                }
            }

            JsonObject result = new JsonObject()
                    .put("code", 200)
                    .put("data", titleList);

            return Future.succeededFuture(result);
        } catch (SQLException e) {
            return Future.failedFuture(String.valueOf(e.getErrorCode()));
        }
    }

    public void stop() {
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}
