/**
 * Copyright 2008 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.katta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import net.sf.katta.client.Client;
import net.sf.katta.client.IClient;
import net.sf.katta.master.IPaths;
import net.sf.katta.master.Master;
import net.sf.katta.slave.Hit;
import net.sf.katta.slave.Hits;
import net.sf.katta.slave.Query;
import net.sf.katta.slave.Slave;
import net.sf.katta.slave.SlaveServerTest;
import net.sf.katta.util.KattaException;
import net.sf.katta.util.ZkConfiguration;
import net.sf.katta.zk.ZKClient;

import org.apache.hadoop.ipc.RPC;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;

public class PerformanceTest extends TestCase {

  final int _hitCount = 200000;

  public static void main(final String[] args) throws InterruptedException, IOException, ParseException, KattaException {
    final PerformanceTest p = new PerformanceTest();
    p.start();
  }

  private void start() throws InterruptedException, IOException, ParseException, KattaException {
    final ZkConfiguration conf = new ZkConfiguration();
    final ZKClient zkclient = new ZKClient(conf);
    final ZkServer server = new ZkServer(conf);
    zkclient.waitForZooKeeper(5000);
    if (zkclient.exists(IPaths.ROOT_PATH)) {
      zkclient.deleteRecursiv(IPaths.ROOT_PATH);
    }
    final Master master = new Master(zkclient);

    final Slave server1 = SlaveServerTest.startSlaveServer(zkclient);
    final Slave server2 = SlaveServerTest.startSlaveServer(zkclient);
    TimingTestUtil.waitFor(zkclient, IPaths.SLAVES, 2);

    final Katta katta = new Katta();
    katta.addIndex("index1", "src/test/testIndexA", StandardAnalyzer.class.getName(), 1);
    katta.addIndex("index2", "src/test/testIndex2", StandardAnalyzer.class.getName(), 1);

    final IClient client = new Client();
    final Query query = new Query("foo: bar");
    long start = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      client.search(query, new String[] { "index2", "index1" });
    }
    System.out.println("search took: " + (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      client.count(query, new String[] { "index2", "index1" });
    }
    System.out.println("count took: " + (System.currentTimeMillis() - start));
    katta.close();
    RPC.stopClient();
    server1.shutdown();
    server2.shutdown();
    Thread.sleep(3000);
    zkclient.close();
    server.shutdown();
  }

  public void testSortSpeed() {
    sortCollection();
    sortMerge();
    // sortOther();
    sortOtherII();
  }

  public void sortCollection() {
    final Random random = new Random();
    // the same number everytime to get comparable results
    random.setSeed(64567547657L);
    final List<Hit> hitList = new ArrayList<Hit>();
    for (int i = 0; i < _hitCount; i++) {
      hitList.add(new Hit("shard", "slave", random.nextFloat(), random.nextInt()));
    }

    final Hits hits = new Hits();
    hits.addHits(hitList);
    final long start = System.currentTimeMillis();
    hits.sortCollection(_hitCount);
    final long end = System.currentTimeMillis();
    System.out.println("sortCollection: " + (end - start) + "ms. for " + _hitCount);
  }

  public void sortMerge() {
    final Random random = new Random();
    // the same number everytime to get comparable results
    random.setSeed(64567547657L);
    final List<Hit> hitList = new ArrayList<Hit>();
    for (int i = 0; i < _hitCount; i++) {
      final Hit hit = new Hit("shard", "slave", random.nextFloat(), random.nextInt());
      hitList.add(hit);
    }

    final Hits hits = new Hits();
    hits.addHits(hitList);
    final long start = System.currentTimeMillis();
    hits.sortMerge();
    final long end = System.currentTimeMillis();
    System.out.println("sortMerge: " + (end - start) + "ms. for " + _hitCount);
  }

  public void sortOther() {
    final Random random = new Random();
    // the same number everytime to get comparable results
    random.setSeed(64567547657L);
    final List<Hit> hitList = new ArrayList<Hit>();
    for (int i = 0; i < _hitCount; i++) {
      final Hit hit = new Hit("shard", "slave", random.nextFloat(), random.nextInt());
      hitList.add(hit);
    }

    final Hits hits = new Hits();
    hits.addHits(hitList);
    final long start = System.currentTimeMillis();
    hits.sortOther();
    final long end = System.currentTimeMillis();
    System.out.println("sortOther: " + (end - start) + "ms. for " + _hitCount);
  }

  public void sortOtherII() {
    final Random random = new Random();
    // the same number everytime to get comparable results
    random.setSeed(64567547657L);
    final List<Hit> hitList = new ArrayList<Hit>();
    for (int i = 0; i < _hitCount; i++) {
      final Hit hit = new Hit("shard", "slave", random.nextFloat(), random.nextInt());
      hitList.add(hit);
    }

    final Hits hits = new Hits();
    hits.addHits(hitList);
    final long start = System.currentTimeMillis();
    hits.sortOtherII();
    final long end = System.currentTimeMillis();
    System.out.println("sortOtherII: " + (end - start) + "ms. for " + _hitCount);
  }
}
