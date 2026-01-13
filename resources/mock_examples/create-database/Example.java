import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.admin.AstraDBAdmin;
import com.dtsx.astra.sdk.db.domain.CloudProviderType;

public class Example {
    public static void main(String[] args) {
        DataAPIClient client = new DataAPIClient("**APPLICATION_TOKEN**");

        AstraDBAdmin admin = client.getAdmin();

        admin.createDatabase("**DATABASE_NAME**", CloudProviderType.GCP, "us-east1");
    }
}
