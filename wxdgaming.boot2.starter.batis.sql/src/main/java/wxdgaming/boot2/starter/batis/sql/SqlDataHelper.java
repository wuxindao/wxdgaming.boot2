package wxdgaming.boot2.starter.batis.sql;

import com.alibaba.fastjson.JSONObject;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import wxdgaming.boot2.core.chatset.json.FastJsonUtil;
import wxdgaming.boot2.core.io.Objects;
import wxdgaming.boot2.starter.batis.DataHelper;
import wxdgaming.boot2.starter.batis.EntityName;
import wxdgaming.boot2.starter.batis.TableMapping;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * 数据集
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-15 12:36
 **/
@Slf4j
@Getter
@Setter
public abstract class SqlDataHelper extends DataHelper {

    protected final SqlConfig sqlConfig;
    protected final SqlDDLBuilder sqlDDLBuilder;
    protected final HikariDataSource hikariDataSource;

    public SqlDataHelper(SqlConfig sqlConfig, SqlDDLBuilder sqlDDLBuilder) {
        this.sqlConfig = sqlConfig;
        this.sqlDDLBuilder = sqlDDLBuilder;
        this.sqlConfig.createDatabase();
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(sqlConfig.getDriverClassName());
        config.setJdbcUrl(sqlConfig.getUrl());
        config.setUsername(sqlConfig.getUsername());
        config.setPassword(sqlConfig.getPassword());
        config.setAutoCommit(true);
        config.setPoolName("wxd.db");
        config.setConnectionTimeout(2000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        config.setValidationTimeout(TimeUnit.SECONDS.toMillis(10));
        config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(3));/*连接存活时间，这个值必须小于 maxLifetime 值。*/
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(6));/*池中连接最长生命周期。*/
        config.setMinimumIdle(sqlConfig.getMinPoolSize());/*池中最小空闲连接数，包括闲置和使用中的连接。*/
        config.setMaximumPoolSize(sqlConfig.getMaxPoolSize());/*池中最大连接数，包括闲置和使用中的连接。*/
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("autoReconnect", "true");
        config.addDataSourceProperty("characterEncoding", "utf-8");
        this.hikariDataSource = new HikariDataSource(config);
    }

    public String getDbName() {
        return sqlConfig.getDbName();
    }

    public void checkTable(Class<?> cls) {
        TableMapping tableMapping = tableMapping(cls);
        if (tableMapping == null) {
            throw new RuntimeException("表映射关系不存在");
        }
        String tableName = tableMapping.getTableName();
        checkTable(tableMapping, tableName, tableMapping.getTableComment());
    }

    public void checkTable(Class<?> cls, String tableName, String tableComment) {
        TableMapping tableMapping = tableMapping(cls);
        if (tableMapping == null) {
            throw new RuntimeException("表映射关系不存在");
        }
        checkTable(tableMapping, tableName, tableComment);
    }

    public void checkTable(TableMapping tableMapping, String tableName, String tableComment) {
        Map<String, LinkedHashMap<String, JSONObject>> tableStructMap = getDbTableStructMap();
        checkTable(tableStructMap, tableMapping, tableName, tableComment);
    }

    public void checkTable(Map<String, LinkedHashMap<String, JSONObject>> databseTableMap, TableMapping tableMapping, String tableName, String tableComment) {
        final LinkedHashMap<String, JSONObject> tableColumns = databseTableMap.get(tableName);
        if (tableColumns == null) {
            createTable(tableMapping, tableName, tableComment);
        } else {
            tableMapping.getColumns().values().forEach(fieldMapping -> {
                JSONObject dbColumnMapping = tableColumns.get(fieldMapping.getColumnName());
                if (dbColumnMapping == null) {
                    addColumn(tableName, fieldMapping);
                } else {
                    updateColumn(tableName, dbColumnMapping, fieldMapping);
                }
            });
        }
    }

    protected void createTable(TableMapping tableMapping, String tableName, String comment) {
        StringBuilder stringBuilder = sqlDDLBuilder.buildTableSqlString(tableMapping, tableName);
        this.executeUpdate(stringBuilder.toString());
        log.warn("创建表：{}", tableName);
    }

    protected void addColumn(String tableName, TableMapping.FieldMapping fieldMapping) {
        String sql = "ALTER TABLE %s ADD COLUMN %s %s COMMENT '%s'".formatted(
                tableName,
                fieldMapping.getColumnName(),
                sqlDDLBuilder.buildColumnDefinition(fieldMapping),
                fieldMapping.getComment()
        );
        executeUpdate(sql);
    }

    protected void updateColumn(String tableName, JSONObject dbColumnMapping, TableMapping.FieldMapping fieldMapping) {
        String columnDefinition = sqlDDLBuilder.buildColumnDefinition(fieldMapping);
        String[] split = columnDefinition.split(" ");
        String columnType = split[0].toLowerCase();
        if (dbColumnMapping.getString("COLUMN_TYPE").equalsIgnoreCase(columnType)) {
            return;
        }
        String sql = "ALTER TABLE %s MODIFY COLUMN %s %s COMMENT '%s';".formatted(
                tableName,
                fieldMapping.getColumnName(),
                columnDefinition,
                fieldMapping.getComment()
        );
        executeUpdate(sql);
    }


    /** 表映射关系 */
    public Map<String, String> getDbTableMap() {
        Map<String, String> dbTableMap = new LinkedHashMap<>();
        String sql = "SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.`TABLES` WHERE table_schema= ? ORDER BY TABLE_NAME";
        this.query(sql, new Object[]{this.getDbName()}, row -> {
            final String table_name = row.getString("TABLE_NAME");
            final String TABLE_COMMENT = row.getString("TABLE_COMMENT");
            dbTableMap.put(table_name, TABLE_COMMENT);
            return false;
        });
        return dbTableMap;
    }

    /** 表映射关系 */
    public Map<String, LinkedHashMap<String, JSONObject>> getDbTableStructMap() {
        LinkedHashMap<String, LinkedHashMap<String, JSONObject>> dbTableStructMap = new LinkedHashMap<>();
        String sql =
                "SELECT" +
                "    TABLE_NAME," +
                "    COLUMN_NAME," +
                "    ORDINAL_POSITION," +
                "    COLUMN_DEFAULT," +
                "    IS_NULLABLE," +
                "    DATA_TYPE," +
                "    CHARACTER_MAXIMUM_LENGTH," +
                "    NUMERIC_PRECISION," +
                "    NUMERIC_SCALE," +
                "    COLUMN_TYPE," +
                "    COLUMN_KEY," +
                "    EXTRA," +
                "    COLUMN_COMMENT \n" +
                "FROM information_schema.`COLUMNS`\n" +
                "WHERE table_schema= ? \n" +
                "ORDER BY TABLE_NAME, ORDINAL_POSITION;";

        this.query(sql, new Object[]{this.getDbName()}, row -> {
            final String table_name = row.getString("TABLE_NAME");
            final String column_name = row.getString("COLUMN_NAME");
            dbTableStructMap
                    .computeIfAbsent(table_name, l -> new LinkedHashMap<>())
                    .put(column_name, row);
            return true;
        });
        return dbTableStructMap;
    }

    @Override public Connection connection() {
        try {
            return hikariDataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int executeUpdate(String sql, Object... params) {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    Object param = params[i];
                    statement.setObject(i + 1, param);
                }
            }
            if (sqlConfig.isDebug()) {
                log.info(
                        "\nexecuteUpdate sql: \n{}",
                        statement.toString()
                );
            }
            int i = statement.executeUpdate();
            if (!connection.getAutoCommit())
                connection.commit();
            return i;
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
    }

    /** 返回第一行，第一列 */
    public <R> R executeScalar(String sql, Class<R> cls, Object... params) {
        AtomicReference<R> ret = new AtomicReference<>();
        this.queryResultSet(sql, params, resultSet -> {
            try {
                Object object = resultSet.getObject(1);
                if (cls.isAssignableFrom(object.getClass())) {
                    ret.set(cls.cast(object));
                } else {
                    ret.set(FastJsonUtil.parse(String.valueOf(object), cls));
                }
                return false;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return ret.get();
    }

    public List<JSONObject> query(String sql, Object... params) {
        List<JSONObject> rows = new ArrayList<>();
        this.query(sql, params, rows::add);
        return rows;
    }

    public void query(String sql, Object[] params, Predicate<JSONObject> consumer) {

        this.queryResultSet(sql, params, resultSet -> {
            try {
                JSONObject jsonObject = new JSONObject();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    jsonObject.put(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
                }
                if (!consumer.test(jsonObject))
                    return false;
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void queryResultSet(String sql, Object[] params, Predicate<java.sql.ResultSet> consumer) {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    Object param = params[i];
                    statement.setObject(i + 1, param);
                }
            }

            if (sqlConfig.isDebug()) {
                log.info(
                        "\nquery sql: \n{}",
                        statement.toString()
                );
            }

            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (!consumer.test(resultSet))
                        break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
    }

    @Override public <R> List<R> findAll(Class<R> cls) {
        TableMapping tableMapping = tableMapping(cls);
        return findAll(tableMapping.getTableName(), cls);
    }

    @Override public <R> List<R> findAll(String tableName, Class<R> cls) {
        TableMapping tableMapping = tableMapping(cls);
        String sql = sqlDDLBuilder.buildSelect(tableMapping, tableMapping.getTableName());
        List<R> ret = new ArrayList<>();
        query(sql, Objects.ZERO_ARRAY, row -> {
            R object = sqlDDLBuilder.data2Object(tableMapping, row);
            ret.add(object);
            return true;
        });
        return ret;
    }

    @Override public <R> R findById(Class<R> cls, Object... args) {
        TableMapping tableMapping = tableMapping(cls);
        return findById(tableMapping.getTableName(), cls, args);
    }

    @Override public <R> R findById(String tableName, Class<R> cls, Object... args) {
        TableMapping tableMapping = tableMapping(cls);
        String sql = sqlDDLBuilder.buildSelect(tableMapping, tableMapping.getTableName());
        String where = sqlDDLBuilder.buildKeyWhere(tableMapping);
        sql += " where " + where;
        AtomicReference<R> ret = new AtomicReference<>();
        this.query(sql, args, row -> {
            R object = sqlDDLBuilder.data2Object(tableMapping, row);
            ret.set(object);
            return false;
        });
        return ret.get();
    }

    @Override public void insert(Object object) {
        TableMapping tableMapping = tableMapping(object.getClass());
        String tableName = tableMapping.getTableName();
        if (object instanceof EntityName entity) {
            tableName = entity.tableName();
        }
        String insert = sqlDDLBuilder.buildInsert(tableMapping, tableName);
        Object[] insertParams = sqlDDLBuilder.buildInsertParams(tableMapping, object);
        this.executeUpdate(insert, insertParams);
    }

    @Override public void update(Object object) {
        TableMapping tableMapping = tableMapping(object.getClass());
        String tableName = tableMapping.getTableName();
        if (object instanceof EntityName entity) {
            tableName = entity.tableName();
        }
        String sql = sqlDDLBuilder.buildUpdate(tableMapping, tableName);
        Object[] objects = sqlDDLBuilder.builderUpdateParams(tableMapping, object);
        this.executeUpdate(sql, objects);
    }

}
