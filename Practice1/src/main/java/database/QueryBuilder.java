package database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class QueryBuilder {

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "name", "price", "quantity", "category"
    );

    public enum Direction {
        ASC, DESC
    }

    private final String baseQuery;
    private final List<String> conditions = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();
    private String orderBy;
    private Integer limit;
    private Integer offset;

    public QueryBuilder(String baseQuery) {
        this.baseQuery = baseQuery;
    }

    public QueryBuilder and(String condition, Object... conditionParams) {
        conditions.add(condition);
        params.addAll(Arrays.asList(conditionParams));
        return this;
    }

    public QueryBuilder andIfNotNull(String condition, Object value) {
        if (value != null) {
            conditions.add(condition);
            params.add(value);
        }
        return this;
    }

    public QueryBuilder orderBy(String column, Direction direction) {
        if (!ALLOWED_COLUMNS.contains(column)) {
            throw new IllegalArgumentException("invalid order by column: " + column);
        }
        this.orderBy = column + " " + direction.name();
        return this;
    }

    public QueryBuilder limit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    public String sql() {
        StringBuilder sb = new StringBuilder(baseQuery);

        if (!conditions.isEmpty()) {
            sb.append(" WHERE ");
            sb.append(String.join(" AND ", conditions));
        }

        if (orderBy != null) {
            sb.append(" ORDER BY ").append(orderBy);
        }

        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
            sb.append(" OFFSET ").append(offset);
        }

        return sb.toString();
    }

    public Object[] params() {
        return params.toArray();
    }

    public QueryBuilder toCountQuery() {
        QueryBuilder count = new QueryBuilder("SELECT COUNT(*) FROM (" + baseQuery + ") AS _count");
        count.conditions.addAll(this.conditions);
        count.params.addAll(this.params);
        return count;
    }
}