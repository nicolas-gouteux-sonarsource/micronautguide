package s3example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

public class ReadAllBabel {

  private static ExecutorService newFixedThreadPoolWithQueueSize(int nThreads, int queueSize) {
    return new ThreadPoolExecutor(nThreads, nThreads,
      5000L, TimeUnit.MILLISECONDS,
      new ArrayBlockingQueue<Runnable>(queueSize, true), new ThreadPoolExecutor.CallerRunsPolicy());
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    String bucketName = "sonarcloud-file-sources";

      S3Client s3Client =  S3Client.builder()
        .credentialsProvider(ProfileCredentialsProvider.create("sonarcloud-dev-admin"))
        //.region(Region.EU_CENTRAL_1)
        .build();


    // list buckets
//    ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
//    ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);
//    listBucketsResponse.buckets().stream().forEach(x -> System.out.println(x.name()));

    // list objects
    ListObjectsV2Request req = ListObjectsV2Request.builder()
      .bucket(bucketName)
      .prefix("AY")
      //.maxKeys(10)
      .build();

    List<String>keys = new ArrayList<>();
    while (true) {
      ListObjectsV2Response listObjResponse = s3Client.listObjectsV2(req);
      for (S3Object s3Object : listObjResponse.contents()) {
        keys.add(s3Object.key());
      }
      if (listObjResponse.nextContinuationToken() == null) {
        break;
      }
      req = req.toBuilder()
        .continuationToken(listObjResponse.nextContinuationToken())
        .build();
    }
    System.out.println("Read " + keys.size() + " keys");


    ExecutorService executorService = newFixedThreadPoolWithQueueSize(200, 100);
    long t0 = System.currentTimeMillis();
    Map<String, byte[]> map = new ConcurrentHashMap<>();
    AtomicInteger counter = new AtomicInteger(0);
    AtomicLong size = new AtomicLong(0);
    // naive read
    for(String key : keys) {
      executorService.submit(() -> {
        byte[] bytes = getObjectAsBytes(s3Client, bucketName, key);
        map.put(key, bytes);
        size.addAndGet(bytes.length);
        int c = counter.incrementAndGet();
        if (c %1000==0) {
          System.out.println(c);
        }
      });
    }
    System.out.println("Finished pushing");
    executorService.shutdown();
    executorService.awaitTermination(5_000, TimeUnit.MINUTES);
    long t1 = System.currentTimeMillis();

    System.out.println(
      String.format("Read %d file sources. SIZE=%d SECONDS=%d",
        map.size(), size.get(), (t1-t0)/1_000));
    


//      GetObjectRequest gor = GetObjectRequest.builder().bucket(bucketName).key(key)
//        .build();
//      ResponseInputStream<GetObjectResponse> r =  s3Client.getObject(gor);
//
//      BufferedReader reader = new BufferedReader(new InputStreamReader(r));
//      String line;
//      while ((line = reader.readLine()) != null) {
//        System.out.println(line);
//      }

  }

  private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
    byte[] b = new byte[size];
    new Random().nextBytes(b);
    return ByteBuffer.wrap(b);
  }

  private static ByteBuffer getRandomString(int size) throws IOException {
    StringBuilder b = new StringBuilder(size);
    Random r = new Random();
    for (int i = 0; i < size; i++) {
      char c = (char) ('A' + r.nextInt(26));
    b.append(c);
    }
    return ByteBuffer.wrap(b.toString().getBytes(StandardCharsets.UTF_8));
  }

  public static byte[] getObjectAsBytes(S3Client s3Client, String bucketName, String key) {
    GetObjectRequest gor = GetObjectRequest.builder().bucket(bucketName).key(key).build();
    ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(gor);
    return objectBytes.asByteArray();
  }

}