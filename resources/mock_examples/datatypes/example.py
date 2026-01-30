from astrapy import DataAPIClient
from astrapy.data_types import DataAPIVector, DataAPIDate, DataAPITime, DataAPIDuration, DataAPIMap, DataAPISet, DataAPITimestamp

print({
    "vector": DataAPIVector([1.0, 2.2, 3.333]),
    "date": DataAPIDate.from_string("2023-10-05"),
    "time": DataAPITime.from_string("14:30:15.123456"),
    "timestamp": DataAPITimestamp.from_string("2023-10-05T14:30:15.123456Z"),
    "map": DataAPIMap({ "key1": "value1", "key2": "value2" }),
    "set": DataAPISet([1, 2, 3, 4, 5.5]),
})
