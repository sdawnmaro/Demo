package constant;

public class SqlQuery {
    public static final String SQL_GET_NEWS_LIST = "SHOW tables";
    public static final String SQL_GET_ARTICLE_NUM(String stationName) {
        return "SELECT COUNT(*) FROM " + stationName;
    }
    public static final String SQL_GET_WORD_NUM(String stationName) {
        return "SELECT SUM(CHAR_LENGTH(text)) AS num FROM " + stationName;
    }
    public static final String SQL_GET_ARTICLE_LIST(String stationName) {
        return "SELECT id, title, url, publish_date FROM " + stationName;
    }
    public static final String SQL_GET_TITLE_LIST(String stationName) {
        return "SELECT id, title, url FROM " + stationName + " WHERE title LIKE ?";
    }
}
