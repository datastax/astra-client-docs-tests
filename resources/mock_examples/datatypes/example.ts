import { vector, date, time } from "@datastax/astra-db-ts";

console.log({
  vector: vector([0.08, -0.62, 0.39]),
  date: date("2023-10-05"),
  time: time("14:30:15.123456"),
  timestamp: new Date("2023-10-05T14:30:15.123Z"),
  map: new Map([["key1", "value1"], ["key2", "value2"]]),
  set: new Set([1, 2, 3, 4, 5.5]),
})
