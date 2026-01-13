from astrapy import DataAPIClient

client = DataAPIClient("**APPLICATION_TOKEN**")

admin = client.get_admin()

admin.create_database("**DATABASE_NAME**", cloud_provider="gcp", region="us-east1")
