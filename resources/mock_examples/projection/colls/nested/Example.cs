using DataStax.AstraDB.DataApi;
using DataStax.AstraDB.DataApi.Collections;
using DataStax.AstraDB.DataApi.Core;
using DataStax.AstraDB.DataApi.Core.Query;

namespace Examples;

public class Program
{
  static void Main()
  {
    // Get an existing collection
    var client = new DataApiClient();
    var database = client.GetDatabase(
      "**API_ENDPOINT**",
      "**APPLICATION_TOKEN**"
    );
    var collection = database.GetCollection("**COLLECTION_NAME**");

    // Use a projection
    var result = collection
      .Find()
      .Project(
        Builders<Document>
          .Projection.Include("metadata.edition")
          .Include("title")
      );
  }
}
