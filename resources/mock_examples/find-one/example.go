package main

import (
	"context"
	"fmt"
	"log"

	"github.com/datastax/astra-db-go/v2/astra"
	"github.com/datastax/astra-db-go/v2/astra/filter"
	"github.com/datastax/astra-db-go/v2/astra/options"
)

func main() {
	// Get an existing collection
	client := astra.NewClient()

	database := client.Database("**API_ENDPOINT**", options.API().SetToken("**APPLICATION_TOKEN**"))

	collection := database.Collection("**COLLECTION_NAME**")

	// Find a document
	filterClause := filter.F{"$and": filter.A{
		filter.F{"is_checked_out": false},
		filter.F{"number_of_pages": filter.F{"$lt": 300}},
	}}

	result := collection.FindOne(context.Background(), filterClause)

	var document map[string]any

	err := result.Decode(&document)
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println(document)
}
