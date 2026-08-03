package interview.mongodb;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static com.mongodb.client.model.Indexes.descending;
import static com.mongodb.client.model.Sorts.orderBy;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.currentDate;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

class MongoCommandShapeTest {
    @Test
    void bsonPreservesLongDateNullAndArrayTypes() {
        Document document = new Document("amount", 5_000_000_000L)
                .append("createdAt", Date.from(Instant.parse("2026-08-02T10:00:00Z")))
                .append("nickname", null)
                .append("tags", List.of("java", "mongodb"));
        BsonDocument bson = document.toBsonDocument();

        assertTrue(bson.get("amount") instanceof BsonInt64);
        assertTrue(bson.get("createdAt").isDateTime());
        assertTrue(bson.get("nickname").isNull());
        assertTrue(bson.get("tags") instanceof BsonArray);
    }

    @Test
    void optimisticUpdateContainsStateAndVersionPredicates() {
        Bson filter = and(eq("_id", "order-1"), eq("status", "CREATED"), eq("version", 3L));
        Bson update = combine(set("status", "PAID"), currentDate("paidAt"), inc("version", 1L));

        BsonDocument filterDocument = bson(filter);
        BsonDocument updateDocument = bson(update);
        BsonArray clauses = filterDocument.getArray("$and");
        assertEquals(new BsonString("CREATED"), clauses.get(1).asDocument().getString("status"));
        assertEquals(new BsonInt64(3L), clauses.get(2).asDocument().getInt64("version"));
        assertEquals(new BsonString("PAID"), updateDocument.getDocument("$set").getString("status"));
        assertEquals(new BsonInt64(1L), updateDocument.getDocument("$inc").getInt64("version"));
        assertTrue(updateDocument.containsKey("$currentDate"));
    }

    @Test
    void uniqueRequestIndexHasExpectedOrderedKeys() {
        IndexModel index = new IndexModel(
                compoundIndex(ascending("customerId"), ascending("requestKey")),
                new IndexOptions().unique(true).name("uq_order_request"));
        BsonDocument keys = bson(index.getKeys());

        assertEquals(List.of("customerId", "requestKey"), keys.keySet().stream().toList());
        assertTrue(index.getOptions().isUnique());
    }

    @Test
    void keysetFilterIncludesBothDescendingTieBreakers() {
        Date time = Date.from(Instant.parse("2026-08-02T10:00:00Z"));
        Bson filter = or(lt("createdAt", time), and(eq("createdAt", time), lt("_id", "order-50")));
        BsonDocument document = bson(filter);

        assertEquals(2, document.getArray("$or").size());
        assertTrue(document.toJson().contains("createdAt"));
        assertTrue(document.toJson().contains("_id"));
    }

    @Test
    void aggregationOrdersFilterGroupSortAndLimit() {
        List<Bson> pipeline = List.of(
                match(eq("status", "PAID")),
                group("$customerId", sum("paidCents", "$totals.subtotalCents")),
                sort(orderBy(descending("paidCents"), ascending("_id"))),
                limit(100));

        assertTrue(bson(pipeline.get(0)).containsKey("$match"));
        assertTrue(bson(pipeline.get(1)).containsKey("$group"));
        assertTrue(bson(pipeline.get(2)).containsKey("$sort"));
        assertEquals(new BsonInt32(100), bson(pipeline.get(3)).getInt32("$limit"));
    }

    @Test
    void transactionOptionsMakeConsistencyIntentVisible() {
        TransactionOptions options = TransactionOptions.builder()
                .readConcern(ReadConcern.SNAPSHOT)
                .writeConcern(WriteConcern.MAJORITY)
                .readPreference(ReadPreference.primary())
                .build();

        assertEquals(ReadConcern.SNAPSHOT, options.getReadConcern());
        assertEquals(WriteConcern.MAJORITY, options.getWriteConcern());
        assertEquals(ReadPreference.primary(), options.getReadPreference());
    }

    @Test
    void compoundCursorIndexMatchesFilterAndSortShape() {
        BsonDocument keys = bson(compoundIndex(
                ascending("customerId"), ascending("status"),
                descending("createdAt"), descending("_id")));
        assertEquals(new BsonInt32(1), keys.getInt32("customerId"));
        assertEquals(new BsonInt32(-1), keys.getInt32("createdAt"));
        assertEquals(new BsonInt32(-1), keys.getInt32("_id"));
    }

    private static BsonDocument bson(Bson value) {
        return value.toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry());
    }
}
