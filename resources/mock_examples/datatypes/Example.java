import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.vector.DataAPIVector;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class Example {
    public static void main(String[] args) {
        System.out.println(new Document(Map.of(
            "vector", new DataAPIVector(new float[]{ 0.08f, -0.62f, 0.39f }),
            "date", new Date(),
            "time", LocalTime.now(),
            "timestamp", Instant.now(),
            "map", Map.of("key1", "value1", "key2", "value2"),
            "set", Set.of(1, 2, 3, 4, 5.5)
        )));
    }
}