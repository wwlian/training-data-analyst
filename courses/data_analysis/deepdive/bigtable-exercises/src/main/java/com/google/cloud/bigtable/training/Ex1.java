// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.cloud.bigtable.training;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.cloud.bigtable.training.common.DataGenerator;
import com.google.cloud.bigtable.training.common.ThreadPoolWriter;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exercise 1 - write some data to Bigtable.
 *
 * Example invocation:
 *
 * mvn compile exec:java -Dexec.mainClass=com.google.cloud.bigtable.training.Ex1 \
 *    -Dbigtable.project=<your project> \
 *    -Dbigtable.instance=<your instance> \
 *    -Dbigtable.table=<any table name> \
 *    -Dexec.cleanupDaemonThreads=false
 */
public class Ex1 {
  public static void main(String[] args) throws Exception {
    String projectId = System.getProperty("bigtable.project");
    String instanceId = System.getProperty("bigtable.instance");
    String tableName = System.getProperty("bigtable.table");

    try (Connection connection = BigtableConfiguration.connect(projectId, instanceId)) {

      // TODO 1a: Implement CreateTable
      CreateTable(connection, tableName);

      final Table table = connection.getTable(TableName.valueOf(tableName));

      final ThreadPoolWriter writer = new ThreadPoolWriter(8);
      final AtomicInteger rowCount = new AtomicInteger();
      long startTime = System.currentTimeMillis();

      // Generate some sample data from some point in the past until now.
      // As our write method gets faster you may want to increase the duration.
      DataGenerator.consumeRandomData(Duration.ofHours(8), point -> {

        try {
          // TODO 1b: Implement SinglePut
          SinglePut(table, writer, point);

          // TODO 1c: Comment out SinglePut, implement and uncomment MultiPut
          // Hint: We are writing with multiple threads to keep Bigtable as busy as possible.
          // Try storing the batches in a ThreadLocal and passing that as an additional parameter to MultiPut.
	  // ThreadLocal<List<Put>> puts = ThreadLocal.withInitial(() -> new ArrayList<>());
          // MultiPut(table, writer, point);

          // TODO 1d: Comment out MultiPut, implement and uncomment WriteWithBufferedMutator.
          // You will need to create a BufferedMutator in the appropriate place and take care to close() it when finished.
          // You will probably want to figure out how to listen for Exceptions from the BufferedMutator for
          // debugging purposes.
          // You might find this method fast enough to consume a lot more data. What about a week's worth?
          // WriteWithBufferedMutator(bufferedMutator, point);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        rowCount.incrementAndGet();
      });

      writer.shutdownAndWait();

      long totalTime = System.currentTimeMillis() - startTime;
      long rps = rowCount.get() / (totalTime / 1000);
      System.out.println("You wrote " + rowCount.get() + " rows at " + rps + " rows per second");

      // TODO: Try running `cbt count <table>` to make sure the actual row count matches
    } catch (IOException e) {
      System.err.println("Exception while running Ex1: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void CreateTable(Connection connection, String tableName) throws Exception {
    Admin admin = connection.getAdmin();

    // TODO 1a: Create a table named with the tableName variable with a column family called "data"
    // and one called "tags". Refer to Ex0 for an example.
    // Delete the table if it already exists for a clean run each time.
    // You can also delete and recreate this table using the cbt tool as needed.
  }

  // TODO 1b: Construct a row key out of the metric name, service and timestamp that efficiently
  //          distributes the data across nodes
  private static String getRowKey(Map<String, Object> point) {
    String metric = point.get(DataGenerator.METRIC_FIELD).toString();
    String service = point.get(DataGenerator.SERVICE_ID_FIELD).toString();
    String ts = point.get(DataGenerator.TIMESTAMP_FIELD).toString();
    return String.join("#", metric, service, ts);
  }

  private static Put getPut(Map<String, Object> point) {
    Put put = new Put(Bytes.toBytes(getRowKey(point)));
    put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("value"),
            Bytes.toBytes(point.get(DataGenerator.VALUE_FIELD).toString()));
    Map<String, String> tags = (Map<String, String>) point.get(DataGenerator.TAGS_FIELD);
    if (tags != null) {
      for (String tag : tags.keySet()) {
        put.addColumn(Bytes.toBytes("tags"), Bytes.toBytes(tag), Bytes.toBytes(tags.get(tag)));
      }
    }
    return put;
  }

  private static void SinglePut(final Table table, ThreadPoolWriter writer, Map<String, Object> point) throws Exception {
    // TODO 1c: For each data point, write a single row into Bigtable.
    // Experiment with the number of threads in the writer to see how Bigtable scales with concurrent writes.
    writer.execute(() -> {
      // Your code here
    }, point);
  }

  private static void MultiPut(final Table table, ThreadPoolWriter writer, Map<String, Object> point) throws Exception {
    // TODO 1d: This time, instead of doing one Put at a time, write in batches using a List of PutsEx1.
    // Experiment with different batch sizes to see the performance differences.
    int batchSize = 10;
    writer.execute(() -> {
      // Your code here
    }, point);
  }

  private static void WriteWithBufferedMutator(BufferedMutator bm, Map<String, Object> point) throws Exception {
    // TODO 1e: Use BufferedMutator
  }
}
