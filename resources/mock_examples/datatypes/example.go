package main

import (
	"fmt"
	"time"

	"github.com/datastax/astra-db-go/v2/astra/datatypes"
)

func main() {
    lm := datatypes.NewLinkedMap[string, string]()
    lm.Set("key1", "value1")
    lm.Set("key2", "value2")

    set := datatypes.NewSet[float32](1, 2, 3, 4, 5.5)

	fmt.Println(map[string]any{
	    "vector": datatypes.NewVector([]float32{0.08, -0.62, 0.39}),
	    "date": datatypes.NewDateOnly(2023, 10, 5),
	    "time": datatypes.NewTimeOnly(14, 30, 15, 123456),
	    "timestamp": time.Date(2023, 10, 5, 14, 30, 15, 123456000, time.UTC),
        "map": lm,
        "set": set,
	})
}
