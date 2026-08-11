package com.example.urlshortener.url.search;

import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.url.entity.ShortUrlEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Repository
public class UrlSearchRepository {
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "createdAt", "url.createdAt",
            "expiresAt", "url.expiresAt",
            "clickCount", "url.clickCount",
            "shortCode", "url.shortCode"
    );

    @PersistenceContext
    private EntityManager entityManager;

    private final Clock clock;

    public UrlSearchRepository(Clock clock) {
        this.clock = clock;
    }

    public List<ShortUrlEntity> search(UUID workspaceId, UrlSearchCriteria criteria) {
        QueryParts parts = build(workspaceId, criteria);
        TypedQuery<ShortUrlEntity> query = entityManager.createQuery(
                "select distinct url from ShortUrlEntity url" + parts.joins() + parts.where() + orderBy(criteria.sort()),
                ShortUrlEntity.class);
        parts.params().forEach(query::setParameter);
        query.setFirstResult(criteria.page() * criteria.size());
        query.setMaxResults(criteria.size());
        return query.getResultList();
    }

    public long count(UUID workspaceId, UrlSearchCriteria criteria) {
        QueryParts parts = build(workspaceId, criteria);
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(distinct url.id) from ShortUrlEntity url" + parts.joins() + parts.where(),
                Long.class);
        parts.params().forEach(query::setParameter);
        return query.getSingleResult();
    }

    private QueryParts build(UUID workspaceId, UrlSearchCriteria criteria) {
        StringBuilder joins = new StringBuilder();
        StringBuilder where = new StringBuilder(" where url.workspace.id = :workspaceId and url.deleted = false");
        Map<String, Object> params = new HashMap<>();
        params.put("workspaceId", workspaceId);

        if (criteria.q() != null && !criteria.q().isBlank()) {
            joins.append(" left join url.campaign qCampaign left join url.tags qTag");
            where.append(" and (lower(url.shortCode) like :qPrefix")
                    .append(" or lower(url.customAlias) like :qPrefix")
                    .append(" or lower(url.destinationHost) like :qPrefix")
                    .append(" or lower(qCampaign.name) like :qPrefix")
                    .append(" or qTag.normalizedName = :qExact)");
            String q = criteria.q().trim().toLowerCase(Locale.ROOT);
            params.put("qPrefix", q + "%");
            params.put("qExact", q);
        }
        if (criteria.state() != null) {
            appendState(where, params, criteria.state());
        }
        if (criteria.campaignId() != null) {
            where.append(" and url.campaign.id = :campaignId");
            params.put("campaignId", criteria.campaignId());
        }
        if (criteria.tag() != null && !criteria.tag().isBlank()) {
            joins.append(" join url.tags filterTag");
            where.append(" and filterTag.normalizedName = :tag");
            params.put("tag", criteria.tag());
        }
        if (criteria.createdFrom() != null) {
            where.append(" and url.createdAt >= :createdFrom");
            params.put("createdFrom", criteria.createdFrom());
        }
        if (criteria.createdTo() != null) {
            where.append(" and url.createdAt <= :createdTo");
            params.put("createdTo", criteria.createdTo());
        }
        if (criteria.expiresBefore() != null) {
            where.append(" and url.expiresAt is not null and url.expiresAt <= :expiresBefore");
            params.put("expiresBefore", criteria.expiresBefore());
        }
        if (criteria.expiresAfter() != null) {
            where.append(" and url.expiresAt is not null and url.expiresAt >= :expiresAfter");
            params.put("expiresAfter", criteria.expiresAfter());
        }
        if (criteria.customAlias() != null) {
            where.append(criteria.customAlias() ? " and url.customAlias is not null" : " and url.customAlias is null");
        }
        return new QueryParts(joins.toString(), where.toString(), params);
    }

    private void appendState(StringBuilder where, Map<String, Object> params, UrlState state) {
        LocalDateTime now = LocalDateTime.now(clock);
        params.put("now", now);
        switch (state) {
            case BLOCKED -> where.append(" and url.blocked = true");
            case EXPIRED -> where.append(" and url.blocked = false and url.expiresAt is not null and url.expiresAt <= :now");
            case DISABLED -> where.append(" and url.blocked = false and (url.expiresAt is null or url.expiresAt > :now) and url.enabled = false");
            case ACTIVE -> where.append(" and url.blocked = false and (url.expiresAt is null or url.expiresAt > :now) and url.enabled = true");
        }
    }

    private String orderBy(String requestedSort) {
        SortSpec sort = parseSort(requestedSort);
        return " order by " + sort.column() + " " + sort.direction() + ", url.id desc";
    }

    private SortSpec parseSort(String requestedSort) {
        if (requestedSort == null || requestedSort.isBlank()) {
            return new SortSpec(SORT_COLUMNS.get("createdAt"), "desc");
        }
        String[] parts = requestedSort.split(",", -1);
        String field = parts[0].trim();
        String column = SORT_COLUMNS.get(field);
        if (column == null) {
            throw new BadRequestException("Unsupported sort field");
        }
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new BadRequestException("Unsupported sort direction");
        }
        return new SortSpec(column, direction);
    }

    private record QueryParts(String joins, String where, Map<String, Object> params) {
    }

    private record SortSpec(String column, String direction) {
    }
}
