CREATE KEYSPACE IF NOT EXISTS aired
  WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 1
  };

USE aired;

CREATE TABLE IF NOT EXISTS vehicle (
    id varchar,
    timestamp timestamp,
    longitude double,
    latitude double,
    tile varchar,
    PRIMARY KEY (id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

CREATE TABLE IF NOT EXISTS tile_vehicle (
    tile_id varchar,
    vehicle_id varchar,
    tile_id_idx varchar,
    PRIMARY KEY (tile_id, vehicle_id)
);
CREATE CUSTOM INDEX tile_id_prefix_search ON tile_vehicle (tile_id_idx) USING 'org.apache.cassandra.index.sasi.SASIIndex';

CREATE TABLE IF NOT EXISTS tile (
    id varchar PRIMARY KEY,
    number_of_vehicles counter
);
